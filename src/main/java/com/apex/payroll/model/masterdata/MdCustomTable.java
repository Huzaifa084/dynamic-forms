package com.apex.payroll.model.masterdata;

import com.apex.payroll.model.AbstractAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "md_custom_tables")
public class MdCustomTable extends AbstractAuditable<Long> {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "table_name", nullable = false, length = 100, unique = true)
    private String tableName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MdTableStatus status = MdTableStatus.DRAFT;
}

