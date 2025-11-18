package com.apex.payroll.repository.masterdata;

import com.apex.payroll.model.masterdata.MdFormBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MdFormBindingRepository extends JpaRepository<MdFormBinding, Long> {

    Optional<MdFormBinding> findByCompanyIdAndFormDefinitionId(UUID companyId, Long formDefinitionId);
}

