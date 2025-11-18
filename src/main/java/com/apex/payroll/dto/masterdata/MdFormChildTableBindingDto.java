package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.UUID;

@Data
public class MdFormChildTableBindingDto {
    private UUID tablePublicId;
    private String fkColumnName;
    private String componentKey;
}

