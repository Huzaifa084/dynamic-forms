package com.apex.payroll.dto.forms;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class FormDefinitionResponse {
    private UUID publicId;
    private String name;
    private String description;
    private Map<String, Object> formDefinition;
    private Integer version;
    private LocalDateTime createdDate;
}
