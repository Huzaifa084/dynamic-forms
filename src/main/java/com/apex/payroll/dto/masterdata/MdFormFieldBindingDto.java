package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.UUID;

@Data
public class MdFormFieldBindingDto {
    private String componentKey;
    private UUID tablePublicId;
    private String columnName;
}

