package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class ReferenceSourceDto {
    private String table;
    private String valueField;
    private String displayField;
    private String filter;
}