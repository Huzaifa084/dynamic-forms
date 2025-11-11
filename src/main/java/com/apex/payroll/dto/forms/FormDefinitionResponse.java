package com.apex.payroll.dto.forms;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FormDefinitionResponse {
    private UUID publicId;
    private String name;
    private String description;
    private FormDefinitionDto formDefinition;
    private Integer version;
    private LocalDateTime createdDate;
}