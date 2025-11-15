package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.ColumnInfoDto;
import com.apex.payroll.dto.forms.TableInfoDto;
import com.apex.payroll.dto.forms.TableMetadataResponse;
import com.apex.payroll.exception.MetadataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetadataService {

    private final DataSource dataSource;

    public TableMetadataResponse getAvailableTables() {
        TableMetadataResponse response = new TableMetadataResponse();

        List<TableInfoDto> tables = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            String schema = safeSchema(conn);

            try (ResultSet rsTables = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rsTables.next()) {
                    String tableSchema = rsTables.getString("TABLE_SCHEM");
                    String tableName = rsTables.getString("TABLE_NAME");

                    if (isSystemSchema(tableSchema)) {
                        continue;
                    }

                    Map<String, String> fkReferences = loadForeignKeyReferences(metaData, schema, tableName);
                    Set<String> primaryKeys = loadPrimaryKeys(metaData, schema, tableName);
                    Set<String> uniqueSingleColumn = loadUniqueSingleColumnIndexes(metaData, schema, tableName);

                    List<ColumnInfoDto> columns = new ArrayList<>();
                    try (ResultSet rsCols = metaData.getColumns(null, schema, tableName, "%")) {
                        while (rsCols.next()) {
                            String colName = rsCols.getString("COLUMN_NAME");
                            String typeName = rsCols.getString("TYPE_NAME");
                            String isNullable = rsCols.getString("IS_NULLABLE");

                            boolean isPk = primaryKeys.contains(colName);
                            boolean isUnique = isPk || uniqueSingleColumn.contains(colName);
                            boolean required = isPk || "NO".equalsIgnoreCase(isNullable);
                            String ref = fkReferences.get(colName);

                            columns.add(createColumnInfo(colName, typeName, isPk, required, isUnique, ref));
                        }
                    }

                    // Sort columns by name for stable output
                    columns = columns.stream()
                            .sorted(Comparator.comparing(ColumnInfoDto::getName))
                            .collect(Collectors.toList());

                    String displayName = toDisplayName(tableName);
                    String description = "Auto-discovered from database";

                    tables.add(createTableInfo(tableName, displayName, description, columns));
                }
            }
        } catch (Exception e) {
            throw new MetadataException("Failed to load table metadata", e);
        }

        // Sort tables alphabetically
        tables.sort(Comparator.comparing(TableInfoDto::getName));
        response.setTables(tables);
        return response;
    }

    private String safeSchema(Connection conn) {
        try {
            String schema = conn.getSchema();
            if (schema == null || schema.isBlank()) {
                return "public"; // default for PostgreSQL
            }
            return schema;
        } catch (Exception e) {
            return "public";
        }
    }

    private boolean isSystemSchema(String schema) {
        if (schema == null) return true;
        String s = schema.toLowerCase(Locale.ROOT);
        return s.startsWith("pg_") || s.equals("pg_catalog") || s.equals("information_schema");
    }

    private Set<String> loadPrimaryKeys(DatabaseMetaData metaData, String schema, String table) throws Exception {
        Set<String> pks = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, schema, table)) {
            while (rs.next()) {
                pks.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pks;
    }

    private Map<String, String> loadForeignKeyReferences(DatabaseMetaData metaData, String schema, String table) throws Exception {
        Map<String, String> refs = new HashMap<>();
        try (ResultSet rs = metaData.getImportedKeys(null, schema, table)) {
            while (rs.next()) {
                String fkCol = rs.getString("FKCOLUMN_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                String pkColumn = rs.getString("PKCOLUMN_NAME");
                refs.put(fkCol, pkTable + "." + pkColumn);
            }
        }
        return refs;
    }

    private Set<String> loadUniqueSingleColumnIndexes(DatabaseMetaData metaData, String schema, String table) throws Exception {
        // Map indexName -> columns in that index
        Map<String, List<String>> indexColumns = new HashMap<>();
        Map<String, Boolean> indexIsUnique = new HashMap<>();

        try (ResultSet rs = metaData.getIndexInfo(null, schema, table, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String colName = rs.getString("COLUMN_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                if (indexName == null || colName == null) continue;
                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(colName);
                indexIsUnique.put(indexName, !nonUnique);
            }
        }

        Set<String> uniqueCols = new HashSet<>();
        for (Map.Entry<String, List<String>> e : indexColumns.entrySet()) {
            String idx = e.getKey();
            List<String> cols = e.getValue();
            Boolean unique = indexIsUnique.getOrDefault(idx, false);
            if (unique != null && unique && cols.size() == 1) {
                uniqueCols.add(cols.getFirst());
            }
        }
        return uniqueCols;
    }

    private String toDisplayName(String name) {
        if (name == null || name.isBlank()) return name;
        String[] parts = name.split("[_\n\r\t ]+");
        return Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(0, 1).toUpperCase(Locale.ROOT) + p.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private TableInfoDto createTableInfo(String name, String displayName, String description, List<ColumnInfoDto> columns) {
        TableInfoDto table = new TableInfoDto();
        table.setName(name);
        table.setDisplayName(displayName);
        table.setDescription(description);
        table.setColumns(columns);
        return table;
    }

    private ColumnInfoDto createColumnInfo(String name, String type, Boolean primaryKey, Boolean required, Boolean unique, String references) {
        ColumnInfoDto column = new ColumnInfoDto();
        column.setName(name);
        column.setType(type);
        column.setPrimaryKey(primaryKey);
        column.setRequired(required);
        column.setUnique(unique);
        column.setReferences(references);
        return column;
    }
}
