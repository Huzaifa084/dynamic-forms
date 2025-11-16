package com.apex.payroll.dto.masterdata;

import com.apex.payroll.model.masterdata.MdDataType;
import lombok.Data;

@Data
public class MdColumnDefinitionDto {
    private String columnName;
    private String displayLabel;
    private MdDataType dataType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private Boolean nullable;
    private Boolean primaryKey;
    private Boolean unique;
    private Boolean indexed;
    private String defaultValue;
}

