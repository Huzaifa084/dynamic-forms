package com.apex.payroll.controller.masterdata;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.masterdata.MdTableDefinitionRequest;
import com.apex.payroll.dto.masterdata.MdTableResponse;
import com.apex.payroll.service.masterdata.MdDynamicTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/md/tables")
@RequiredArgsConstructor
@Tag(name = "Master Data Tables", description = "Define and manage master data tables")
public class MdDynamicTableController {

    private final MdDynamicTableService tableService;

    @PostMapping("/preview")
    @Operation(summary = "Preview the CREATE TABLE SQL for a master data table without applying it")
    public BaseResponseEntity<String> previewCreateTable(@RequestBody MdTableDefinitionRequest request) {
        String sql = tableService.previewCreateTableSql(request);
        return ResponseBuilder.success(sql, "Preview generated successfully");
    }

    @PostMapping
    @Operation(summary = "Create and apply a master data table for the given company")
    public BaseResponseEntity<MdTableResponse> createAndApply(@RequestBody MdTableDefinitionRequest request) {
        MdTableResponse response = tableService.applyCreateTable(request);
        return ResponseBuilder.success(response, "Master data table created successfully");
    }

    @GetMapping
    @Operation(summary = "List all master data tables defined for the current company")
    public BaseResponseEntity<List<MdTableResponse>> listTables(@RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {
        companyId = setCompanyIdIfNull(companyId); // TODO: For dev only
        List<MdTableResponse> tables = tableService.listTables(companyId);
        return ResponseBuilder.success(tables);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a master data table definition with its columns and relationships")
    public BaseResponseEntity<MdTableResponse> getTable(@PathVariable Long id) {
        MdTableResponse table = tableService.getTable(id);
        return ResponseBuilder.success(table);
    }

    // TODO: For dev only
    private UUID setCompanyIdIfNull(UUID companyId) {
        if (companyId == null) {
            return UUID.fromString("5a0c4535-d39d-4a1f-b847-b2717ca3640f");
        }
        return companyId;
    }
}