package com.apex.payroll.controller;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.forms.TableDataResponse;
import com.apex.payroll.dto.forms.TableMetadataResponse;
import com.apex.payroll.service.forms.MetadataService;
import com.apex.payroll.service.forms.ReferenceDataService;
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
        try {
            TableMetadataResponse tables = metadataService.getAvailableTables();
            return ResponseBuilder.success(tables);
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }
    
    @GetMapping("/tables/{tableName}/data")
    public BaseResponseEntity<TableDataResponse> getTableData(
            @PathVariable String tableName,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestHeader("X-Company-ID") UUID companyId) {
        try {
            TableDataResponse response = new TableDataResponse();
            response.setTableName(tableName);
            response.setData(referenceDataService.getReferenceData(tableName, search, companyId, limit));
            response.setTotalCount((long) response.getData().size());
            
            return ResponseBuilder.success(response);
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }
}