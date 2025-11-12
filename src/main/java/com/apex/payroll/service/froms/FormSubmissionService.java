package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.SubmitFormRequest;
import com.apex.payroll.model.FormDefinition;
import com.apex.payroll.model.FormSubmission;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.FormSubmissionRepository;
import com.apex.payroll.util.JsonHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FormSubmissionService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormDefinitionService formDefinitionService;
    private final FormValidationService formValidationService;
    private final ReferenceDataService referenceDataService;
    private final EntityManager em;

    public FormSubmission submitForm(UUID formDefinitionPublicId, SubmitFormRequest request, UUID companyId, User currentUser) {
        FormDefinition formDefinition = formDefinitionService.getFormDefinitionByPublicId(formDefinitionPublicId, companyId);

        // Validate submission data against form definition
        Map<String, Object> formData = request.getData();
        formValidationService.validateFormSubmission(formDefinition.getFormDefinitionJson(), formData);

        // Process reference fields
        referenceDataService.validateReferenceFields(formDefinition.getFormDefinitionJson(), formData, companyId);

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setCompanyId(companyId);
        submission.setData(JsonHelper.toJson(formData));
        submission.setSearchableText(extractSearchableText(formData));
        if (currentUser != null && currentUser.getId() != null) {
            submission.setSubmittedBy(em.getReference(User.class, currentUser.getId()));
        }

        return formSubmissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public FormSubmission getFormSubmissionByPublicId(UUID publicId, UUID companyId) {
        return formSubmissionRepository.findByPublicIdAndCompanyId(publicId, companyId)
                .orElseThrow(() -> new RuntimeException("Form submission not found"));
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
}
