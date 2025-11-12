package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateFormDefinitionRequest {
    private String display;              // e.g., "form"
    private String formName;             // logical name of the form
    private List<Map<String, Object>> components; // arbitrary components list
    private String description;
}

