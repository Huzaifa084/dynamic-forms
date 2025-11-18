package com.apex.payroll.dto.masterdata;

import lombok.Data;

@Data
public class MdChildTableBindingDto {
    private Long tableId;
    private String fkColumnName;
    private String componentKey;
}

