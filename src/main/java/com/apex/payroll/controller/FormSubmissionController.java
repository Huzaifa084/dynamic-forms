package com.apex.payroll.controller;

import com.apex.payroll.dto.base.BaseResponse;
import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.dto.forms.*;
import com.apex.payroll.model.FormSubmission;
import com.apex.payroll.model.User;
import com.apex.payroll.service.forms.FormSubmissionService;
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
            @RequestHeader("X-Company-ID") UUID companyId,
            @AuthenticationPrincipal User currentUser) {
        try {
            FormSubmission submission = formSubmissionService.submitForm(formDefinitionPublicId, request, companyId, currentUser);
            return ResponseBuilder.success(toResponse(submission), "Form submitted successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(e.getMessage());
        }
    }
    
    @GetMapping("/{publicId}")
    public BaseResponseEntity<FormSubmissionResponse> getFormSubmission(
            @PathVariable UUID publicId,
            @RequestHeader("X-Company-ID") UUID companyId) {
        try {
            FormSubmission submission = formSubmissionService.getFormSubmissionByPublicId(publicId, companyId);
            return ResponseBuilder.success(toResponse(submission));
        } catch (Exception e) {
            return ResponseBuilder.notFound("publicId", "Form submission not found");
        }
    }
    
    @GetMapping
    public BaseResponseEntity<List<FormSubmissionResponse>> getFormSubmissions(
            @RequestParam UUID formDefinitionPublicId,
            @RequestHeader("X-Company-ID") UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        try {
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
            @RequestHeader("X-Company-ID") UUID companyId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
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
}