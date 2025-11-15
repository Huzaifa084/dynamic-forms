package com.apex.payroll.dto.masterdata;

import com.apex.payroll.model.masterdata.MdTableStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class MdTableResponse {
    private Long id;
    private UUID companyId;
    private String schemaName;
    private String tableName;
    private String displayName;
    private Integer version;
    private MdTableStatus status;
    private LocalDateTime createdDate;
    private List<MdColumnDefinitionDto> columns;
    private List<MdRelationshipDefinitionDto> relationships;
}

