package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class TableDataRequest {
    private String search;
    private Integer limit = 50;
}