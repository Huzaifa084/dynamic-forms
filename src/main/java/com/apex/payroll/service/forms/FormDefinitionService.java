package com.apex.payroll.service.forms;

import com.apex.payroll.dto.forms.*;
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
    
    public FormDefinition createFormDefinition(CreateFormDefinitionRequest request, UUID companyId, User currentUser) {
        // Validate form definition
        formValidationService.validateFormDefinition(request.getFormDefinition());
        
        // Check for duplicate names
        if (formDefinitionRepository.existsByNameAndCompanyId(request.getName(), companyId)) {
            throw new RuntimeException("Form with name '" + request.getName() + "' already exists");
        }
        
        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setCompanyId(companyId);
        formDefinition.setName(request.getName());
        formDefinition.setDescription(request.getDescription());
        formDefinition.setFormDefinitionJson(JsonHelper.toJson(request.getFormDefinition()));
        formDefinition.setVersion(1);
        formDefinition.setCreatedBy(currentUser);
        
        return formDefinitionRepository.save(formDefinition);
    }
    
    @Transactional(readOnly = true)
    public FormDefinition getFormDefinitionByPublicId(UUID publicId, UUID companyId) {
        return formDefinitionRepository.findByPublicIdAndCompanyId(publicId, companyId)
                .orElseThrow(() -> new RuntimeException("Form definition not found"));
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
    
    public FormDefinition updateFormDefinition(UUID publicId, CreateFormDefinitionRequest request, UUID companyId, User currentUser) {
        FormDefinition existing = getFormDefinitionByPublicId(publicId, companyId);
        
        // Validate
        formValidationService.validateFormDefinition(request.getFormDefinition());
        
        // Update fields
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setFormDefinitionJson(JsonHelper.toJson(request.getFormDefinition()));
        existing.setVersion(existing.getVersion() + 1);
        existing.setLastModifiedBy(currentUser);
        
        return formDefinitionRepository.save(existing);
    }
    
    public void deleteFormDefinition(UUID publicId, UUID companyId) {
        FormDefinition formDefinition = getFormDefinitionByPublicId(publicId, companyId);
        formDefinitionRepository.delete(formDefinition);
    }
}