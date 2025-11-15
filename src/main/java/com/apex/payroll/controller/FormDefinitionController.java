package com.apex.payroll.controller;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.forms.CreateFormDefinitionRequest;
import com.apex.payroll.dto.forms.FormDefinitionResponse;
import com.apex.payroll.model.FormDefinition;
import com.apex.payroll.model.User;
import com.apex.payroll.service.froms.FormDefinitionService;
import com.apex.payroll.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms/definitions")
@RequiredArgsConstructor
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;

    @PostMapping
    public BaseResponseEntity<FormDefinitionResponse> createFormDefinition(
            @RequestBody CreateFormDefinitionRequest request,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @AuthenticationPrincipal User currentUser) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            FormDefinition formDefinition = formDefinitionService.createFormDefinition(request, companyId, currentUser);
            return ResponseBuilder.success(toResponse(formDefinition), "Form definition created successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    @GetMapping("/{publicId}")
    public BaseResponseEntity<FormDefinitionResponse> getFormDefinition(
            @PathVariable UUID publicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            FormDefinition formDefinition = formDefinitionService.getFormDefinitionByPublicId(publicId, companyId);
            return ResponseBuilder.success(toResponse(formDefinition));
        } catch (Exception e) {
            return ResponseBuilder.notFound("publicId", "Form definition not found");
        }
    }

    @GetMapping
    public BaseResponseEntity<List<FormDefinitionResponse>> getFormDefinitions(
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<FormDefinition> formDefinitions = formDefinitionService.searchFormDefinitions(companyId, search, pageable);

            Page<FormDefinitionResponse> response = formDefinitions.map(this::toResponseTemp);
            return ResponseBuilder.success(response.getContent(), page, response.getTotalElements(), response.getTotalPages());
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    @PutMapping("/{publicId}")
    public BaseResponseEntity<FormDefinitionResponse> updateFormDefinition(
            @PathVariable UUID publicId,
            @RequestBody CreateFormDefinitionRequest request,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @AuthenticationPrincipal User currentUser) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            FormDefinition formDefinition = formDefinitionService.updateFormDefinition(publicId, request, companyId, currentUser);
            return ResponseBuilder.success(toResponse(formDefinition), "Form definition updated successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    @DeleteMapping("/{publicId}")
    public BaseResponseEntity<Void> deleteFormDefinition(
            @PathVariable UUID publicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            formDefinitionService.deleteFormDefinition(publicId, companyId);
            return ResponseBuilder.success("Form definition deleted successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    private FormDefinitionResponse toResponse(FormDefinition formDefinition) {
        FormDefinitionResponse response = new FormDefinitionResponse();
        response.setPublicId(formDefinition.getPublicId());
        response.setName(formDefinition.getName());
        response.setDescription(formDefinition.getDescription());
        response.setFormDefinition(JsonHelper.fromJson(formDefinition.getFormDefinitionJson(), java.util.Map.class));
        response.setVersion(formDefinition.getVersion());
        response.setCreatedDate(formDefinition.getCreatedDate().orElse(null));
        return response;
    }

    // TODO: Temp
    private FormDefinitionResponse toResponseTemp(FormDefinition formDefinition) {
        FormDefinitionResponse response = new FormDefinitionResponse();
        response.setPublicId(formDefinition.getPublicId());
        response.setName(formDefinition.getName());
        response.setDescription(formDefinition.getDescription());
        response.setVersion(formDefinition.getVersion());
        response.setCreatedDate(formDefinition.getCreatedDate().orElse(null));
        return response;
    }

    // TODO: For dev only
    private UUID setCompanyIdIfNull(UUID companyId) {
        if (companyId == null) {
            return UUID.fromString("5a0c4535-d39d-4a1f-b847-b2717ca3640f");
        }
        return companyId;
    }
}