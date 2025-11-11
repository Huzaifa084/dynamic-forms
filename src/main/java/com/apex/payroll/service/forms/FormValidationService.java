package com.apex.payroll.service.forms;

import com.apex.payroll.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class FormValidationService {

    public void validateFormDefinition(Object formDefinition) {
        // Basic validation logic
        // Check required sections, field types, etc.
        // Throw RuntimeException with validation errors
    }

    public void validateFormSubmission(String formDefinitionJson, Map<String, Object> formData) {
        Map<String, Object> formDefinition = JsonHelper.fromJson(formDefinitionJson, Map.class);
        // Validate required fields, data types, etc.
        // Throw RuntimeException with validation errors
    }
}


