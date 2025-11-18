package com.apex.payroll.repository.masterdata;

import com.apex.payroll.model.masterdata.MdCustomTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MdCustomTableRepository extends JpaRepository<MdCustomTable, Long> {

    List<MdCustomTable> findByCompanyId(UUID companyId);

    Optional<MdCustomTable> findByPublicIdAndCompanyId(UUID publicId, UUID companyId);

    boolean existsByTableName(String tableName);
}

