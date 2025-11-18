package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MdFormBindingResponse {
    private Long id;
    private UUID companyId;
    private UUID formDefinitionPublicId;
    private Long primaryTableId;
    private String type;
    private List<MdFieldMappingDto> mappings;
    private List<MdChildTableBindingDto> childTables;
}

