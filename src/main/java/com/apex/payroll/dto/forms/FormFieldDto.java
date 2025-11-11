package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;

@Data
public class FormFieldDto {
    private String fieldId;
    private String type; // text, number, email, reference, etc.
    private String label;
    private String placeholder;
    private String description;
    private Boolean required = false;
    private Integer displayOrder = 0;
    private Integer columnWidth = 12;
    private FieldValidationDto validations;
    private ReferenceSourceDto source; // For reference fields
    private List<String> options; // For dropdown/radio
}