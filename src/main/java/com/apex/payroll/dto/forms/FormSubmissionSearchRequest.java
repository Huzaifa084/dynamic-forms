package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class FormSubmissionSearchRequest {
    private String searchTerm;
    private Integer page = 0;
    private Integer size = 50;
    private String sortBy = "submittedAt";
    private String sortDirection = "DESC";
}