package com.apex.payroll.dto.masterdata;

import com.apex.payroll.model.masterdata.MdFkAction;
import com.apex.payroll.model.masterdata.MdRelationType;
import lombok.Data;

@Data
public class MdRelationshipDefinitionDto {
    private String sourceTableName;
    private String sourceColumnName;
    private String targetTableName;
    private String targetColumnName;
    private MdRelationType relationType;
    private MdFkAction onDelete;
    private MdFkAction onUpdate;
}

