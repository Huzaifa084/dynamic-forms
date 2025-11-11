package com.apex.payroll.dto.forms;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class FormSubmissionResponse {
    private UUID publicId;
    private UUID formDefinitionPublicId;
    private String formName;
    private Map<String, Object> data;
    private LocalDateTime submittedAt;
    private String submittedBy;
}