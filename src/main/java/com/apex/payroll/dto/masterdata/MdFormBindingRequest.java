package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;

@Data
public class MdFormBindingRequest {
    private Long primaryTableId;
    private List<MdFieldMappingDto> mappings;
    private List<MdChildTableBindingDto> childTables;
}