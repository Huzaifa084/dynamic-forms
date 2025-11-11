package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class ColumnInfoDto {
    private String name;
    private String type;
    private Boolean primaryKey = false;
    private Boolean required = false;
    private Boolean unique = false;
    private String references;
}