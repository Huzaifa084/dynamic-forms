package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class FormSubmissionWithDefinitionResponse {

    private FormDefinitionResponse formDefinition;
    private FormSubmissionResponse submission;
}

