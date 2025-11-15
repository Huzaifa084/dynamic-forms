package com.apex.payroll.controller.masterdata;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.masterdata.MdTableDefinitionRequest;
import com.apex.payroll.dto.masterdata.MdTableResponse;
import com.apex.payroll.service.masterdata.MdDynamicTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/md/tables")
@RequiredArgsConstructor
public class MdDynamicTableController {

    private final MdDynamicTableService tableService;

    @PostMapping("/preview")
    public BaseResponseEntity<String> previewCreateTable(@RequestBody MdTableDefinitionRequest request) {
        String sql = tableService.previewCreateTableSql(request);
        return ResponseBuilder.success(sql, "Preview generated successfully");
    }

    @PostMapping
    public BaseResponseEntity<MdTableResponse> createAndApply(@RequestBody MdTableDefinitionRequest request) {
        MdTableResponse response = tableService.applyCreateTable(request);
        return ResponseBuilder.success(response, "Master data table created successfully");
    }

    @GetMapping
    public BaseResponseEntity<List<MdTableResponse>> listTables(@RequestParam UUID companyId) {
        List<MdTableResponse> tables = tableService.listTables(companyId);
        return ResponseBuilder.success(tables);
    }

    @GetMapping("/{id}")
    public BaseResponseEntity<MdTableResponse> getTable(@PathVariable Long id) {
        MdTableResponse table = tableService.getTable(id);
        return ResponseBuilder.success(table);
    }
}

