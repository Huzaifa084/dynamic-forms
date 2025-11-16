package com.apex.payroll.repository.masterdata;

import com.apex.payroll.model.masterdata.MdCustomRelationship;
import com.apex.payroll.model.masterdata.MdCustomTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MdCustomRelationshipRepository extends JpaRepository<MdCustomRelationship, Long> {

    List<MdCustomRelationship> findBySourceTable(MdCustomTable table);
}

