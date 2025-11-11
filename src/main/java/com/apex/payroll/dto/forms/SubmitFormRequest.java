package com.apex.payroll.dto.forms;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitFormRequest {
    private Map<String, Object> data;
}
