package com.apex.payroll.model.masterdata;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "md_custom_relationships")
public class MdCustomRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_table_id", nullable = false)
    private MdCustomTable sourceTable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_column_id", nullable = false)
    private MdCustomColumn sourceColumn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_table_id", nullable = false)
    private MdCustomTable targetTable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_column_id", nullable = false)
    private MdCustomColumn targetColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private MdRelationType relationType;

    @Column(name = "on_delete_action", length = 30)
    private String onDelete;

    @Column(name = "on_update_action", length = 30)
    private String onUpdate;
}

