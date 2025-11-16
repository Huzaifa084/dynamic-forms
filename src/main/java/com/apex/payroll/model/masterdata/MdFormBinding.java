package com.apex.payroll.model.masterdata;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "md_form_bindings")
public class MdFormBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "form_definition_id", nullable = false)
    private Long formDefinitionId;

    @Column(name = "primary_table_id", nullable = false)
    private Long primaryTableId;

    @Type(JsonType.class)
    @Column(name = "binding_json", columnDefinition = "jsonb", nullable = false)
    private String bindingJson;
}