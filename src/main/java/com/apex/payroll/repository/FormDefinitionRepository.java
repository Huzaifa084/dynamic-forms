package com.apex.payroll.repository;

import com.apex.payroll.model.FormDefinition;
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
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

    Optional<FormDefinition> findByPublicIdAndCompanyId(UUID publicId, UUID companyId);

    List<FormDefinition> findByCompanyId(UUID companyId);

    Page<FormDefinition> findByCompanyId(UUID companyId, Pageable pageable);

    boolean existsByNameAndCompanyId(String name, UUID companyId);

    @Query("SELECT fd FROM FormDefinition fd WHERE fd.companyId = :companyId AND LOWER(fd.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<FormDefinition> searchByCompanyIdAndName(
            @Param("companyId") UUID companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}