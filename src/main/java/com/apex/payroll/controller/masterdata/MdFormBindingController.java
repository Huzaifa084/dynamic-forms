package com.apex.payroll.controller.masterdata;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.masterdata.MdFormBindingRequest;
import com.apex.payroll.dto.masterdata.MdFormBindingResponse;
import com.apex.payroll.service.masterdata.MdFormBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/md/forms")
@RequiredArgsConstructor
@Tag(name = "Master Data Form Bindings", description = "Link form definitions to master data tables")
public class MdFormBindingController {

    private final MdFormBindingService formBindingService;

    @PostMapping("/{formDefinitionPublicId}/binding")
    @Operation(summary = "Create or update a master data binding for a form definition")
    public BaseResponseEntity<MdFormBindingResponse> upsertBinding(
            @PathVariable UUID formDefinitionPublicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @RequestBody MdFormBindingRequest request) {

        companyId = setCompanyIdIfNull(companyId); // TODO: For dev only
        MdFormBindingResponse resp = formBindingService.upsertBinding(companyId, formDefinitionPublicId, request);
        return ResponseBuilder.success(resp, "Form binding saved successfully");
    }

    @GetMapping("/{formDefinitionPublicId}/binding")
    @Operation(summary = "Get the master data binding for a form definition")
    public BaseResponseEntity<MdFormBindingResponse> getBinding(
            @PathVariable UUID formDefinitionPublicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {

        companyId = setCompanyIdIfNull(companyId); // TODO: For dev only
        MdFormBindingResponse resp = formBindingService.getBinding(companyId, formDefinitionPublicId);
        return ResponseBuilder.success(resp);
    }

    // TODO: For dev only
    private UUID setCompanyIdIfNull(UUID companyId) {
        return UUID.fromString("5a0c4535-d39d-4a1f-b847-b2717ca3640f");
    }
}

