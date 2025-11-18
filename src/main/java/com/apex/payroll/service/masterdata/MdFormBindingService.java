package com.apex.payroll.service.masterdata;

import com.apex.payroll.dto.masterdata.MdChildTableBindingDto;
import com.apex.payroll.dto.masterdata.MdFieldMappingDto;
import com.apex.payroll.dto.masterdata.MdFormBindingRequest;
import com.apex.payroll.dto.masterdata.MdFormBindingResponse;
import com.apex.payroll.exception.BadRequestException;
import com.apex.payroll.exception.ResourceNotFoundException;
import com.apex.payroll.model.FormDefinition;
import com.apex.payroll.model.masterdata.MdCustomColumn;
import com.apex.payroll.model.masterdata.MdCustomTable;
import com.apex.payroll.model.masterdata.MdFormBinding;
import com.apex.payroll.model.masterdata.MdTableStatus;
import com.apex.payroll.repository.FormDefinitionRepository;
import com.apex.payroll.repository.masterdata.MdCustomColumnRepository;
import com.apex.payroll.repository.masterdata.MdCustomTableRepository;
import com.apex.payroll.repository.masterdata.MdFormBindingRepository;
import com.apex.payroll.util.JsonHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MdFormBindingService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final MdCustomTableRepository tableRepository;
    private final MdCustomColumnRepository columnRepository;
    private final MdFormBindingRepository bindingRepository;

    public MdFormBindingResponse upsertBinding(
            UUID companyId,
            UUID formDefinitionPublicId,
            MdFormBindingRequest request) {

        FormDefinition formDefinition = formDefinitionRepository
                .findByPublicIdAndCompanyId(formDefinitionPublicId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form definition not found"));

        MdCustomTable primaryTable = tableRepository.findById(request.getPrimaryTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary master data table not found"));
        if (!primaryTable.getCompanyId().equals(companyId)) {
            throw new BadRequestException("Primary table does not belong to this company");
        }
        if (primaryTable.getStatus() != MdTableStatus.APPLIED) {
            throw new BadRequestException("Primary table must be in APPLIED status before binding");
        }

        validateMappings(companyId, request);

        BindingConfig config = new BindingConfig();
        config.setType("MASTER_DATA");
        config.setPrimaryTableId(request.getPrimaryTableId());
        config.setMappings(request.getMappings());
        config.setChildTables(request.getChildTables());

        String json = JsonHelper.toJson(config);

        Optional<MdFormBinding> existingOpt =
                bindingRepository.findByCompanyIdAndFormDefinitionId(companyId, formDefinition.getId());

        MdFormBinding entity = existingOpt.orElseGet(MdFormBinding::new);
        entity.setCompanyId(companyId);
        entity.setFormDefinitionId(formDefinition.getId());
        entity.setPrimaryTableId(request.getPrimaryTableId());
        entity.setBindingJson(json);

        MdFormBinding saved = bindingRepository.save(entity);
        log.info("MdFormBindingService.upsertBinding - saved binding id={} for formDefinitionPublicId={} and primaryTableId={}",
                saved.getId(), formDefinitionPublicId, saved.getPrimaryTableId());

        return toResponse(saved, formDefinitionPublicId);
    }

    @Transactional(readOnly = true)
    public MdFormBindingResponse getBinding(UUID companyId, UUID formDefinitionPublicId) {
        FormDefinition formDefinition = formDefinitionRepository
                .findByPublicIdAndCompanyId(formDefinitionPublicId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form definition not found"));

        MdFormBinding binding = bindingRepository
                .findByCompanyIdAndFormDefinitionId(companyId, formDefinition.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Form binding not found"));

        return toResponse(binding, formDefinitionPublicId);
    }

    private void validateMappings(UUID companyId, MdFormBindingRequest request) {
        if (request.getMappings() != null) {
            for (MdFieldMappingDto m : request.getMappings()) {
                MdCustomTable table = tableRepository.findById(m.getTableId())
                        .orElseThrow(() -> new BadRequestException("Unknown table in mapping: " + m.getTableId()));
                if (!table.getCompanyId().equals(companyId)) {
                    throw new BadRequestException("Mapping table does not belong to this company: " + m.getTableId());
                }
                if (table.getStatus() != MdTableStatus.APPLIED) {
                    throw new BadRequestException("Mapping table must be in APPLIED status: " + table.getTableName());
                }
                MdCustomColumn col = columnRepository.findByTable(table).stream()
                        .filter(c -> normalizeName(m.getColumnName()).equals(c.getColumnName()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Unknown column in mapping: " + m.getColumnName()));
            }
        }

        if (request.getChildTables() != null) {
            for (MdChildTableBindingDto c : request.getChildTables()) {
                MdCustomTable table = tableRepository.findById(c.getTableId())
                        .orElseThrow(() -> new BadRequestException("Unknown child table: " + c.getTableId()));
                if (!table.getCompanyId().equals(companyId)) {
                    throw new BadRequestException("Child table does not belong to this company: " + c.getTableId());
                }
                if (table.getStatus() != MdTableStatus.APPLIED) {
                    throw new BadRequestException("Child table must be in APPLIED status: " + table.getTableName());
                }
                MdCustomColumn fkCol = columnRepository.findByTable(table).stream()
                        .filter(col -> normalizeName(c.getFkColumnName()).equals(col.getColumnName()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Unknown FK column for child table: " + c.getFkColumnName()));
            }
        }
    }

    private MdFormBindingResponse toResponse(MdFormBinding binding, UUID formDefinitionPublicId) {
        BindingConfig config = JsonHelper.fromJson(binding.getBindingJson(), BindingConfig.class);

        MdFormBindingResponse resp = new MdFormBindingResponse();
        resp.setId(binding.getId());
        resp.setCompanyId(binding.getCompanyId());
        resp.setFormDefinitionPublicId(formDefinitionPublicId);
        resp.setPrimaryTableId(binding.getPrimaryTableId());
        resp.setType(config.getType());
        resp.setMappings(config.getMappings());
        resp.setChildTables(config.getChildTables());
        return resp;
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    // Internal config structure persisted as JSON in MdFormBinding.bindingJson
    @Setter
    @Getter
    private static class BindingConfig {
        private String type;
        private Long primaryTableId;
        private List<MdFieldMappingDto> mappings;
        private List<MdChildTableBindingDto> childTables;

    }
}

