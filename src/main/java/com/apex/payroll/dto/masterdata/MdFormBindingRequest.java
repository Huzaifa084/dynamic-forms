package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MdFormBindingRequest {
    private UUID primaryTablePublicId;
    private List<MdFormFieldBindingDto> mappings;
    private List<MdFormChildTableBindingDto> childTables;
}
