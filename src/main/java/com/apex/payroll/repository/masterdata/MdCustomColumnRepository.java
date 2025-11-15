package com.apex.payroll.repository.masterdata;

import com.apex.payroll.model.masterdata.MdCustomColumn;
import com.apex.payroll.model.masterdata.MdCustomTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MdCustomColumnRepository extends JpaRepository<MdCustomColumn, Long> {

    List<MdCustomColumn> findByTable(MdCustomTable table);
}

