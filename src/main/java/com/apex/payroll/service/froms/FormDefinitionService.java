package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.*;
import com.apex.payroll.exception.ResourceAlreadyExistsException;
import com.apex.payroll.exception.ResourceNotFoundException;
import com.apex.payroll.model.FormDefinition;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.FormDefinitionRepository;
import com.apex.payroll.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FormDefinitionService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormValidationService formValidationService;

    public FormDefinition createFormDefinition(
            CreateFormDefinitionRequest request, UUID companyId, User currentUser
    ) {
        formValidationService.validateFormDefinition(request);
        if (formDefinitionRepository.existsByNameAndCompanyId(request.getFormName(), companyId)) {
            throw new ResourceAlreadyExistsException("Form with name '" + request.getFormName() + "' already exists");
        }

        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setCompanyId(companyId);
        formDefinition.setName(request.getFormName());
        formDefinition.setDescription(request.getDescription());
        // Persist the entire schema as JSON
        formDefinition.setFormDefinitionJson(JsonHelper.toJson(request));
        formDefinition.setVersion(1);
        formDefinition.setCreatedBy(currentUser);

        return formDefinitionRepository.save(formDefinition);
    }

    @Transactional(readOnly = true)
    public FormDefinition getFormDefinitionByPublicId(UUID publicId, UUID companyId) {
        return formDefinitionRepository.findByPublicIdAndCompanyId(publicId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form definition not found"));
    }

    @Transactional(readOnly = true)
    public List<FormDefinition> getFormDefinitionsByCompany(UUID companyId) {
        return formDefinitionRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Page<FormDefinition> searchFormDefinitions(UUID companyId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return formDefinitionRepository.findByCompanyId(companyId, pageable);
        }
        return formDefinitionRepository.searchByCompanyIdAndName(companyId, searchTerm.trim(), pageable);
    }

    public FormDefinition updateFormDefinition(
            UUID publicId, CreateFormDefinitionRequest request, UUID companyId, User currentUser
    ) {
        FormDefinition existing = getFormDefinitionByPublicId(publicId, companyId);

        // Validate
        formValidationService.validateFormDefinition(request);

        // Update fields
        existing.setName(request.getFormName());
        existing.setDescription(request.getDescription());
        existing.setFormDefinitionJson(JsonHelper.toJson(request));
        existing.setVersion(existing.getVersion() + 1);
        existing.setLastModifiedBy(currentUser);

        return formDefinitionRepository.save(existing);
    }

    public void deleteFormDefinition(UUID publicId, UUID companyId) {
        FormDefinition formDefinition = getFormDefinitionByPublicId(publicId, companyId);
        formDefinitionRepository.delete(formDefinition);
    }
}