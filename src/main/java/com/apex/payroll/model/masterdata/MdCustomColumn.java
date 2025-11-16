package com.apex.payroll.model.masterdata;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "md_custom_columns")
public class MdCustomColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private MdCustomTable table;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "display_label", nullable = false, length = 200)
    private String displayLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private MdDataType dataType;

    @Column(name = "length")
    private Integer length;

    @Column(name = "precision_val")
    private Integer precision;

    @Column(name = "scale_val")
    private Integer scale;

    @Column(name = "nullable", nullable = false)
    private Boolean nullable = Boolean.TRUE;

    @Column(name = "primary_key", nullable = false)
    private Boolean primaryKey = Boolean.FALSE;

    @Column(name = "unique_val", nullable = false)
    private Boolean unique = Boolean.FALSE;

    @Column(name = "indexed", nullable = false)
    private Boolean indexed = Boolean.FALSE;

    @Column(name = "default_value")
    private String defaultValue;
}