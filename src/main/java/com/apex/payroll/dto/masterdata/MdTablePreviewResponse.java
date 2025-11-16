package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;

@Data
public class MdTablePreviewResponse {

    private String schemaName;
    private String tableName;
    private List<MdColumnDefinitionDto> columns;
    private List<MdRelationshipDefinitionDto> relationships;
    private String createTableSql;
}

