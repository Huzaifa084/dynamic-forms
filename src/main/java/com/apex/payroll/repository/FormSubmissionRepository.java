package com.apex.payroll.repository;

import com.apex.payroll.model.FormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {

    Optional<FormSubmission> findByPublicIdAndCompanyId(UUID publicId, UUID companyId);

    Page<FormSubmission> findByFormDefinitionPublicIdAndCompanyId(UUID formDefinitionPublicId, UUID companyId, Pageable pageable);

    @Query("SELECT fs FROM FormSubmission fs WHERE fs.formDefinition.publicId = :formDefinitionPublicId AND fs.companyId = :companyId " +
            "AND fs.searchableText ILIKE %:searchTerm%")
    Page<FormSubmission> searchByFormAndText(
            @Param("formDefinitionPublicId") UUID formDefinitionPublicId,
            @Param("companyId") UUID companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    @Query(value = "SELECT * FROM form_submissions WHERE form_definition_id = :formDefinitionId AND company_id = :companyId " +
            "AND data ->> :fieldPath = :value", nativeQuery = true)
    List<FormSubmission> findByFieldValue(
            @Param("formDefinitionId") Long formDefinitionId,
            @Param("companyId") UUID companyId,
            @Param("fieldPath") String fieldPath,
            @Param("value") String value);

    @Query(value = "SELECT * FROM form_submissions WHERE company_id = :companyId " +
            "AND to_tsvector('english', searchable_text) @@ plainto_tsquery('english', :searchTerm)",
            countQuery = "SELECT count(*) FROM form_submissions WHERE company_id = :companyId " +
                    "AND to_tsvector('english', searchable_text) @@ plainto_tsquery('english', :searchTerm)",
            nativeQuery = true)
    Page<FormSubmission> fullTextSearch(
            @Param("companyId") UUID companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}