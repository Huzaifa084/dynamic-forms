package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.SubmitFormRequest;
import com.apex.payroll.exception.BadRequestException;
import com.apex.payroll.exception.ResourceNotFoundException;
import com.apex.payroll.model.FormDefinition;
import com.apex.payroll.model.FormSubmission;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.FormSubmissionRepository;
import com.apex.payroll.util.JsonHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FormSubmissionService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormDefinitionService formDefinitionService;
    private final FormValidationService formValidationService;
    private final ReferenceDataService referenceDataService;
    private final EntityManager em;

    public FormSubmission submitForm(UUID formDefinitionPublicId, SubmitFormRequest request, UUID companyId, User currentUser) {
        FormDefinition formDefinition = formDefinitionService
                .getFormDefinitionByPublicId(formDefinitionPublicId, companyId);

        Map<String, Object> formData = extractAndValidateFormData(formDefinition, request, companyId);
        String dataJson = JsonHelper.toJson(formData);
        log.info("FormSubmissionService.submitForm - about to save submission for formDefinitionPublicId={}, companyId={}, dataJson={}",
                formDefinitionPublicId, companyId, truncateForLog(dataJson));

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setCompanyId(companyId);
        submission.setData(dataJson);
        submission.setSearchableText(extractSearchableText(formData));
        if (currentUser != null && currentUser.getId() != null) {
            submission.setSubmittedBy(em.getReference(User.class, currentUser.getId()));
        }

        FormSubmission saved = formSubmissionRepository.save(submission);
        log.info("FormSubmissionService.submitForm - saved submission id={}, publicId={}, dataJson={}",
                saved.getId(), saved.getPublicId(), truncateForLog(saved.getData()));
        return saved;
    }

    public FormSubmission updateFormSubmission(UUID publicId, SubmitFormRequest request, UUID companyId, User currentUser) {
        FormSubmission existing = getFormSubmissionByPublicId(publicId, companyId);
        FormDefinition formDefinition = existing.getFormDefinition();

        Map<String, Object> formData = extractAndValidateFormData(formDefinition, request, companyId);
        String dataJson = JsonHelper.toJson(formData);
        log.info("FormSubmissionService.updateFormSubmission - updating submission publicId={}, companyId={}, newDataJson={}",
                publicId, companyId, truncateForLog(dataJson));

        existing.setData(dataJson);
        existing.setSearchableText(extractSearchableText(formData));
        // Keep original submittedBy; auditing will track lastModifiedBy

        FormSubmission saved = formSubmissionRepository.save(existing);
        log.info("FormSubmissionService.updateFormSubmission - saved submission id={}, publicId={}, dataJson={}",
                saved.getId(), saved.getPublicId(), truncateForLog(saved.getData()));
        return saved;
    }

    @Transactional(readOnly = true)
    public FormSubmission getFormSubmissionByPublicId(UUID publicId, UUID companyId) {
        FormSubmission submission = formSubmissionRepository.findByPublicIdAndCompanyId(publicId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form submission not found"));
        log.info("FormSubmissionService.getFormSubmissionByPublicId - loaded submission publicId={}, companyId={}, rawData={}",
                publicId, companyId, truncateForLog(submission.getData()));
        return submission;
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> getFormSubmissions(UUID formDefinitionPublicId, UUID companyId, Pageable pageable) {
        return formSubmissionRepository.findByFormDefinitionPublicIdAndCompanyId(formDefinitionPublicId, companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> searchFormSubmissions(UUID formDefinitionPublicId, UUID companyId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getFormSubmissions(formDefinitionPublicId, companyId, pageable);
        }
        return formSubmissionRepository.searchByFormAndText(formDefinitionPublicId, companyId, searchTerm.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> fullTextSearch(UUID companyId, String searchTerm, Pageable pageable) {
        return formSubmissionRepository.fullTextSearch(companyId, searchTerm, pageable);
    }

    private String extractSearchableText(Map<String, Object> data) {
        StringBuilder searchable = new StringBuilder();
        flattenDataForSearch(data, searchable);
        String out = searchable.toString().trim();
        // Guard against DB column length limits (commonly 255 for String)
        if (out.length() > 255) {
            out = out.substring(0, 255);
        }
        return out;
    }

    private void flattenDataForSearch(Object data, StringBuilder result) {
        if (data instanceof Map) {
            ((Map<?, ?>) data).values().forEach(value -> flattenDataForSearch(value, result));
        } else if (data instanceof Iterable) {
            ((Iterable<?>) data).forEach(value -> flattenDataForSearch(value, result));
        } else if (data != null) {
            String s = data.toString();
            // Avoid appending extremely long tokens (e.g., base64 signatures)
            if (s.length() > 256) s = s.substring(0, 256);
            result.append(s).append(" ");
        }
    }

    private String truncateForLog(String s) {
        if (s == null) return "null";
        int max = 500;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    private Map<String, Object> extractAndValidateFormData(FormDefinition formDefinition,
                                                           SubmitFormRequest request,
                                                           UUID companyId) {
        Map<String, Object> formData = request.getData();
        if (formData == null) {
            throw new BadRequestException("Request body must contain a 'data' object with form field values, e.g. { \"data\": { ... } }");
        }

        // Validate submission data against form definition
        formValidationService.validateFormSubmission(formDefinition.getFormDefinitionJson(), formData);
        // Process reference fields
        referenceDataService.validateReferenceFields(formDefinition.getFormDefinitionJson(), formData, companyId);
        return formData;
    }
}
