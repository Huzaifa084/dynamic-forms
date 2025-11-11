package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.List;

@Data
public class SectionDto {
    private String sectionId;
    private String title;
    private String description;
    private List<FormFieldDto> fields;
}