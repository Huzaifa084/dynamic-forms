package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;

@Data
public class TableMetadataResponse {
    private List<TableInfoDto> tables;
}