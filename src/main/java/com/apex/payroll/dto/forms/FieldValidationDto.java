package com.apex.payroll.dto.forms;

import lombok.Data;

@Data
public class FieldValidationDto {
    private Integer minLength;
    private Integer maxLength;
    private Integer minValue;
    private Integer maxValue;
    private String pattern;
    private String customMessage;
}