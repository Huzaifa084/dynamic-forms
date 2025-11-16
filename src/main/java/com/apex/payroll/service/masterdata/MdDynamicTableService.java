package com.apex.payroll.service.masterdata;

import com.apex.payroll.dto.masterdata.MdColumnDefinitionDto;
import com.apex.payroll.dto.masterdata.MdRelationshipDefinitionDto;
import com.apex.payroll.dto.masterdata.MdTableDefinitionRequest;
import com.apex.payroll.dto.masterdata.MdTablePreviewResponse;
import com.apex.payroll.dto.masterdata.MdTableResponse;
import com.apex.payroll.exception.BadRequestException;
import com.apex.payroll.model.masterdata.*;
import com.apex.payroll.repository.masterdata.MdCustomColumnRepository;
import com.apex.payroll.repository.masterdata.MdCustomRelationshipRepository;
import com.apex.payroll.repository.masterdata.MdCustomTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MdDynamicTableService {

    private static final String DEFAULT_SCHEMA = "dynamic_forms";
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    // System-managed columns: technical PK + auditing-style columns (do not allow frontend to define these)
    private static final Set<String> RESERVED_SYSTEM_COLUMNS = Set.of(
            "id",
            "created_by",
            "created_date",
            "last_modified_by",
            "last_modified_date"
    );

    private final MdCustomTableRepository tableRepository;
    private final MdCustomColumnRepository columnRepository;
    private final MdCustomRelationshipRepository relationshipRepository;
    private final JdbcTemplate jdbcTemplate;

    public MdTablePreviewResponse previewCreateTable(MdTableDefinitionRequest request) {
        validateDefinition(request, true);

        MdTablePreviewResponse preview = new MdTablePreviewResponse();
        preview.setSchemaName(DEFAULT_SCHEMA);
        preview.setTableName(normalizeName(request.getTableName()));
        preview.setColumns(request.getColumns());
        preview.setRelationships(request.getRelationships());
        preview.setCreateTableSql(buildCreateTableSql(request));
        return preview;
    }

    @Transactional
    public MdTableResponse applyCreateTable(MdTableDefinitionRequest request) {
        validateDefinition(request, false);

        MdCustomTable table = new MdCustomTable();
        table.setCompanyId(request.getCompanyId());
        table.setSchemaName(DEFAULT_SCHEMA);
        table.setTableName(normalizeName(request.getTableName()));
        table.setDisplayName(request.getDisplayName());
        table.setStatus(MdTableStatus.DRAFT);

        table = tableRepository.save(table);

        Map<String, MdCustomColumn> columnByName = new LinkedHashMap<>();
        for (MdColumnDefinitionDto c : request.getColumns()) {
            MdCustomColumn col = new MdCustomColumn();
            col.setTable(table);
            col.setColumnName(normalizeName(c.getColumnName()));
            col.setDisplayLabel(c.getDisplayLabel());
            col.setDataType(c.getDataType());
            col.setLength(c.getLength());
            col.setPrecision(c.getPrecision());
            col.setScale(c.getScale());
            col.setNullable(Boolean.TRUE.equals(c.getNullable()));
            col.setPrimaryKey(Boolean.TRUE.equals(c.getPrimaryKey()));
            col.setUnique(Boolean.TRUE.equals(c.getUnique()));
            col.setIndexed(Boolean.TRUE.equals(c.getIndexed()));
            col.setDefaultValue(c.getDefaultValue());

            col = columnRepository.save(col);
            columnByName.put(col.getColumnName(), col);
        }

        List<MdCustomRelationship> relationships = new ArrayList<>();
        if (request.getRelationships() != null) {
            for (MdRelationshipDefinitionDto relDto : request.getRelationships()) {
                // For now, only allow relationships where this table is the source
                if (!normalizeName(relDto.getSourceTableName()).equals(table.getTableName())) {
                    continue;
                }
                MdCustomRelationship rel = new MdCustomRelationship();
                rel.setSourceTable(table);
                MdCustomColumn srcCol = columnByName.get(normalizeName(relDto.getSourceColumnName()));
                if (srcCol == null) {
                    throw new BadRequestException("Unknown source column in relationship: " + relDto.getSourceColumnName());
                }
                rel.setSourceColumn(srcCol);
                // Target table/column must already exist and be APPLIED
                MdCustomTable targetTable = tableRepository.findAll().stream()
                        .filter(t -> normalizeName(relDto.getTargetTableName()).equals(t.getTableName()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Unknown target table: " + relDto.getTargetTableName()));
                rel.setTargetTable(targetTable);
                MdCustomColumn targetColumn = columnRepository.findByTable(targetTable).stream()
                        .filter(c -> normalizeName(relDto.getTargetColumnName()).equals(c.getColumnName()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Unknown target column: " + relDto.getTargetColumnName()));
                rel.setTargetColumn(targetColumn);

                rel.setRelationType(relDto.getRelationType());
                rel.setOnDelete(relDto.getOnDelete());
                rel.setOnUpdate(relDto.getOnUpdate());

                relationships.add(relationshipRepository.save(rel));
            }
        }

        String ddl = buildCreateTableSql(request);
        try {
            ensureSchemaExists(DEFAULT_SCHEMA);
            jdbcTemplate.execute(ddl);
            for (String alter : buildRelationshipSql(table, relationships)) {
                jdbcTemplate.execute(alter);
            }
            table.setStatus(MdTableStatus.APPLIED);
            table = tableRepository.save(table);
        } catch (Exception e) {
            log.error("Failed to apply DDL for table {}: {}", table.getTableName(), e.getMessage(), e);
            table.setStatus(MdTableStatus.ERROR);
            tableRepository.save(table);
            throw new BadRequestException("Failed to apply table definition: " + e.getMessage());
        }

        return toResponse(table, new ArrayList<>(columnByName.values()), relationships);
    }

    public List<MdTableResponse> listTables(UUID companyId) {
        return tableRepository.findByCompanyId(companyId).stream()
                .map(t -> {
                    List<MdCustomColumn> cols = columnRepository.findByTable(t);
                    List<MdCustomRelationship> rels = relationshipRepository.findBySourceTable(t);
                    return toResponse(t, cols, rels);
                })
                .collect(Collectors.toList());
    }

    public MdTableResponse getTable(Long id) {
        MdCustomTable t = tableRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Master data table not found: " + id));
        List<MdCustomColumn> cols = columnRepository.findByTable(t);
        List<MdCustomRelationship> rels = relationshipRepository.findBySourceTable(t);
        return toResponse(t, cols, rels);
    }

    // --- Validation ---

    private void validateDefinition(MdTableDefinitionRequest req, boolean preview) {
        if (req.getCompanyId() == null) {
            throw new BadRequestException("companyId is required");
        }
        if (isBlank(req.getTableName()) || !NAME_PATTERN.matcher(req.getTableName()).matches()) {
            throw new BadRequestException("Invalid tableName. Use letters, digits, and underscore; must not start with digit.");
        }
        if (isBlank(req.getDisplayName())) {
            throw new BadRequestException("displayName is required");
        }
        if (req.getColumns() == null || req.getColumns().isEmpty()) {
            throw new BadRequestException("At least one column definition is required");
        }

        String normalizedTable = normalizeName(req.getTableName());
        if (!preview && tableRepository.existsByTableName(normalizedTable)) {
            throw new BadRequestException("Table with name '" + normalizedTable + "' already exists");
        }

        Set<String> seenColumns = new HashSet<>();
        for (MdColumnDefinitionDto col : req.getColumns()) {
            if (isBlank(col.getColumnName()) || !NAME_PATTERN.matcher(col.getColumnName()).matches()) {
                throw new BadRequestException("Invalid columnName: " + col.getColumnName());
            }
            String normalized = normalizeName(col.getColumnName());
            if (RESERVED_SYSTEM_COLUMNS.contains(normalized)) {
                throw new BadRequestException("Column name '" + normalized + "' is reserved and added automatically by the system");
            }
            if (!seenColumns.add(normalized)) {
                throw new BadRequestException("Duplicate columnName: " + normalized);
            }
            if (col.getDataType() == null) {
                throw new BadRequestException("dataType is required for column: " + normalized);
            }
            validateColumnType(col);
        }
    }

    private void validateColumnType(MdColumnDefinitionDto col) {
        MdDataType type = col.getDataType();
        Integer length = col.getLength();
        Integer precision = col.getPrecision();
        Integer scale = col.getScale();

        switch (type) {
            case STRING -> {
                if (length == null || length <= 0) {
                    throw new BadRequestException("STRING column '" + col.getColumnName() + "' requires positive length");
                }
            }
            case DECIMAL -> {
                if (precision == null || precision <= 0) {
                    throw new BadRequestException("DECIMAL column '" + col.getColumnName() + "' requires precision");
                }
                if (scale == null || scale < 0 || scale > precision) {
                    throw new BadRequestException("DECIMAL column '" + col.getColumnName() + "' requires scale between 0 and precision");
                }
            }
            case INTEGER, BIGINT, DATE, TIMESTAMP, BOOLEAN, JSONB, TEXT -> {
                // no specific numeric length rules
            }
            default -> throw new BadRequestException("Unsupported data type: " + type);
        }
    }

    // --- SQL generation ---

    private String buildCreateTableSql(MdTableDefinitionRequest req) {
        String tableName = quoteIdentifier(DEFAULT_SCHEMA) + "." + quoteIdentifier(normalizeName(req.getTableName()));

        List<String> columnDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();
        List<String> uniqueConstraints = new ArrayList<>();

        // Technical primary key & auditing columns (added implicitly, not part of the client definition)
        columnDefs.add(quoteIdentifier("id") + " BIGSERIAL");
        pkColumns.add(quoteIdentifier("id"));
        columnDefs.add(quoteIdentifier("created_by") + " BIGINT");
        columnDefs.add(quoteIdentifier("created_date") + " TIMESTAMP NOT NULL DEFAULT now()");
        columnDefs.add(quoteIdentifier("last_modified_by") + " BIGINT");
        columnDefs.add(quoteIdentifier("last_modified_date") + " TIMESTAMP");

        for (MdColumnDefinitionDto col : req.getColumns()) {
            String colName = normalizeName(col.getColumnName());
            StringBuilder sb = new StringBuilder();
            sb.append(quoteIdentifier(colName)).append(" ").append(sqlTypeFor(col));

            if (!Boolean.TRUE.equals(col.getNullable())) {
                sb.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null && !col.getDefaultValue().isBlank()) {
                sb.append(" DEFAULT ").append(renderDefault(col));
            }
            columnDefs.add(sb.toString());

            // Treat primaryKey flag on user columns as a uniqueness requirement; technical PK is always 'id'
            if (Boolean.TRUE.equals(col.getPrimaryKey()) || Boolean.TRUE.equals(col.getUnique())) {
                uniqueConstraints.add("UNIQUE (" + quoteIdentifier(colName) + ")");
            }
        }

        if (!pkColumns.isEmpty()) {
            columnDefs.add("PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }
        columnDefs.addAll(uniqueConstraints);

        return "CREATE TABLE " + tableName + " (\n  " +
                String.join(",\n  ", columnDefs) +
                "\n);";
    }

    private List<String> buildRelationshipSql(MdCustomTable table, List<MdCustomRelationship> relationships) {
        List<String> ddls = new ArrayList<>();
        for (MdCustomRelationship rel : relationships) {
            String srcTable = qualifiedTableName(table.getSchemaName(), table.getTableName());
            String tgtTable = qualifiedTableName(rel.getTargetTable().getSchemaName(), rel.getTargetTable().getTableName());

            String constraintName = "fk_" + table.getTableName() + "_" + rel.getSourceColumn().getColumnName();

            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ").append(srcTable)
                    .append(" ADD CONSTRAINT ").append(quoteIdentifier(constraintName))
                    .append(" FOREIGN KEY (").append(quoteIdentifier(rel.getSourceColumn().getColumnName())).append(")")
                    .append(" REFERENCES ").append(tgtTable)
                    .append(" (").append(quoteIdentifier(rel.getTargetColumn().getColumnName())).append(")");

            if (rel.getOnDelete() != null && !rel.getOnDelete().isBlank()) {
                sb.append(" ON DELETE ").append(rel.getOnDelete());
            }
            if (rel.getOnUpdate() != null && !rel.getOnUpdate().isBlank()) {
                sb.append(" ON UPDATE ").append(rel.getOnUpdate());
            }
            sb.append(";");
            ddls.add(sb.toString());
        }
        return ddls;
    }

    private void ensureSchemaExists(String schema) {
        String ddl = "CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schema);
        jdbcTemplate.execute(ddl);
    }

    private String sqlTypeFor(MdColumnDefinitionDto col) {
        MdDataType type = col.getDataType();
        return switch (type) {
            case STRING -> "VARCHAR(" + col.getLength() + ")";
            case TEXT -> "TEXT";
            case INTEGER -> "INTEGER";
            case BIGINT -> "BIGINT";
            case DECIMAL -> "DECIMAL(" + col.getPrecision() + "," + col.getScale() + ")";
            case DATE -> "DATE";
            case TIMESTAMP -> "TIMESTAMP";
            case BOOLEAN -> "BOOLEAN";
            case JSONB -> "JSONB";
        };
    }

    private String renderDefault(MdColumnDefinitionDto col) {
        MdDataType type = col.getDataType();
        String dv = col.getDefaultValue();
        return switch (type) {
            case STRING, TEXT, DATE, TIMESTAMP, JSONB -> "'" + dv.replace("'", "''") + "'";
            case BOOLEAN -> dv.equalsIgnoreCase("true") ? "TRUE" : "FALSE";
            case INTEGER, BIGINT, DECIMAL -> dv;
        };
    }

    private String quoteIdentifier(String id) {
        return "\"" + id.replace("\"", "\"\"") + "\"";
    }

    private String qualifiedTableName(String schema, String table) {
        return quoteIdentifier(schema) + "." + quoteIdentifier(table);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private MdTableResponse toResponse(
            MdCustomTable table,
            List<MdCustomColumn> columns,
            List<MdCustomRelationship> relationships
    ) {
        MdTableResponse resp = new MdTableResponse();
        resp.setId(table.getId());
        resp.setCompanyId(table.getCompanyId());
        resp.setSchemaName(table.getSchemaName());
        resp.setTableName(table.getTableName());
        resp.setDisplayName(table.getDisplayName());
        resp.setVersion(table.getVersion());
        resp.setStatus(table.getStatus());
        resp.setCreatedDate(table.getCreatedDate().orElse(null));

        List<MdColumnDefinitionDto> colDtos = columns.stream().map(c -> {
            MdColumnDefinitionDto dto = new MdColumnDefinitionDto();
            dto.setColumnName(c.getColumnName());
            dto.setDisplayLabel(c.getDisplayLabel());
            dto.setDataType(c.getDataType());
            dto.setLength(c.getLength());
            dto.setPrecision(c.getPrecision());
            dto.setScale(c.getScale());
            dto.setNullable(c.getNullable());
            dto.setPrimaryKey(c.getPrimaryKey());
            dto.setUnique(c.getUnique());
            dto.setIndexed(c.getIndexed());
            dto.setDefaultValue(c.getDefaultValue());
            return dto;
        }).collect(Collectors.toList());
        resp.setColumns(colDtos);

        List<MdRelationshipDefinitionDto> relDtos = relationships.stream().map(r -> {
            MdRelationshipDefinitionDto dto = new MdRelationshipDefinitionDto();
            dto.setSourceTableName(r.getSourceTable().getTableName());
            dto.setSourceColumnName(r.getSourceColumn().getColumnName());
            dto.setTargetTableName(r.getTargetTable().getTableName());
            dto.setTargetColumnName(r.getTargetColumn().getColumnName());
            dto.setRelationType(r.getRelationType());
            dto.setOnDelete(r.getOnDelete());
            dto.setOnUpdate(r.getOnUpdate());
            return dto;
        }).collect(Collectors.toList());
        resp.setRelationships(relDtos);

        return resp;
    }
}
