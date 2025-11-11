package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class CreateFormDefinitionRequest {
    private String name;
    private String description;
    private FormDefinitionDto formDefinition;
}

