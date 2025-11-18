package com.apex.payroll.dto.masterdata;

import lombok.Data;

@Data
public class MdFieldMappingDto {
    private String componentKey;
    private Long tableId;
    private String columnName;
}

