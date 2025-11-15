package com.apex.payroll.controller;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.forms.TableDataResponse;
import com.apex.payroll.dto.forms.TableMetadataResponse;
import com.apex.payroll.service.froms.MetadataService;
import com.apex.payroll.service.froms.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
public class MetadataController {
    
    private final MetadataService metadataService;
    private final ReferenceDataService referenceDataService;
    
    @GetMapping("/tables")
    public BaseResponseEntity<TableMetadataResponse> getAvailableTables() {
        TableMetadataResponse tables = metadataService.getAvailableTables();
        return ResponseBuilder.success(tables);
    }
    
    @GetMapping("/tables/{tableName}/data")
    public BaseResponseEntity<TableDataResponse> getTableData(
            @PathVariable String tableName,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {
        companyId = setCompanyIdIfNull(companyId);
        TableDataResponse response = new TableDataResponse();
        response.setTableName(tableName);
        response.setData(referenceDataService.getReferenceData(tableName, search, companyId, limit));
        response.setTotalCount((long) response.getData().size());

        return ResponseBuilder.success(response);
    }
    // TODO: For dev only
    private UUID setCompanyIdIfNull(UUID companyId) {
        if (companyId == null) {
            return UUID.fromString("5a0c4535-d39d-4a1f-b847-b2717ca3640f");
        }
        return companyId;
    }
}
