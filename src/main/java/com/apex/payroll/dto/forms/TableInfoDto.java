package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;

@Data
public class TableInfoDto {
    private String name;
    private String displayName;
    private String description;
    private List<ColumnInfoDto> columns;
}