package com.apex.payroll.controller;

import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.forms.*;
import com.apex.payroll.model.FormSubmission;
import com.apex.payroll.model.User;
import com.apex.payroll.service.froms.FormSubmissionService;
import com.apex.payroll.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms/submissions")
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    @PostMapping("/{formDefinitionPublicId}")
    public BaseResponseEntity<FormSubmissionResponse> submitForm(
            @PathVariable UUID formDefinitionPublicId,
            @RequestBody SubmitFormRequest request,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @AuthenticationPrincipal User currentUser) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            FormSubmission submission = formSubmissionService.submitForm(formDefinitionPublicId, request, companyId, currentUser);
            return ResponseBuilder.success(toResponse(submission), "Form submitted successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    @GetMapping("/{publicId}")
    public BaseResponseEntity<FormSubmissionResponse> getFormSubmission(
            @PathVariable UUID publicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            FormSubmission submission = formSubmissionService.getFormSubmissionByPublicId(publicId, companyId);
            return ResponseBuilder.success(toResponse(submission));
        } catch (Exception e) {
            return ResponseBuilder.notFound("publicId", "Form submission not found");
        }
    }

    @GetMapping
    public BaseResponseEntity<List<FormSubmissionResponse>> getFormSubmissions(
            @RequestParam UUID formDefinitionPublicId,
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
            Page<FormSubmission> submissions = formSubmissionService.searchFormSubmissions(formDefinitionPublicId, companyId, search, pageable);

            Page<FormSubmissionResponse> response = submissions.map(this::toResponse);
            return ResponseBuilder.success(response.getContent(), page, response.getTotalElements(), response.getTotalPages());
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    public BaseResponseEntity<List<FormSubmissionResponse>> searchAllSubmissions(
            @RequestHeader(value = "X-Company-ID", required = false) UUID companyId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            companyId = setCompanyIdIfNull(companyId);
            Pageable pageable = PageRequest.of(page, size); // Native query handles ordering by submitted_at DESC
            Page<FormSubmission> submissions = formSubmissionService.fullTextSearch(companyId, q, pageable);

            Page<FormSubmissionResponse> response = submissions.map(this::toResponse);
            return ResponseBuilder.success(response.getContent(), page, response.getTotalElements(), response.getTotalPages());
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }

    private FormSubmissionResponse toResponse(FormSubmission submission) {
        FormSubmissionResponse response = new FormSubmissionResponse();
        response.setPublicId(submission.getPublicId());
        response.setFormDefinitionPublicId(submission.getFormDefinition().getPublicId());
        response.setFormName(submission.getFormDefinition().getName());
        response.setData(JsonHelper.fromJson(submission.getData(), Map.class));
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setSubmittedBy(submission.getSubmittedBy() != null ?
                submission.getSubmittedBy().getFullName() : "System");
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
