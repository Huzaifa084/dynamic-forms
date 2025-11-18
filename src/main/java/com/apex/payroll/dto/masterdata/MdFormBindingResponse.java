package com.apex.payroll.dto.masterdata;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MdFormBindingResponse {
    private UUID companyId;
    private UUID formDefinitionPublicId;
    private UUID primaryTablePublicId;
    private String type;
    private List<MdFormFieldBindingDto> mappings;
    private List<MdFormChildTableBindingDto> childTables;
}
