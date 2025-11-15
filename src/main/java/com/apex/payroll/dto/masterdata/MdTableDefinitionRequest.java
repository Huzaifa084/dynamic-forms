package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MdTableDefinitionRequest {
    private UUID companyId;
    private String tableName;
    private String displayName;
    private List<MdColumnDefinitionDto> columns;
    private List<MdRelationshipDefinitionDto> relationships;
}

