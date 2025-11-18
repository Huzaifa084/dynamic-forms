package com.apex.payroll.dto.masterdata;

import com.apex.payroll.model.masterdata.MdTableStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MdTableSummaryResponse {
    private UUID publicId;
    private UUID companyId;
    private String schemaName;
    private String tableName;
    private String displayName;
    private Integer version;
    private MdTableStatus status;
    private LocalDateTime createdDate;
}

