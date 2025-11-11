package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TableDataResponse {
    private String tableName;
    private List<Map<String, Object>> data;
    private Long totalCount;
}