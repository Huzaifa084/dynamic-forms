package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormDefinitionDto {
    private List<SectionDto> sections;
    private Map<String, Object> validations;
    private Map<String, Object> uiConfig;
}