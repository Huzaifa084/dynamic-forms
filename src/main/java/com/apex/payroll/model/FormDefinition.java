package com.apex.payroll.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "form_definitions")
public class FormDefinition extends AbstractAuditable<Long> {

    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Type(JsonType.class)
    @Column(name = "form_definition_json", columnDefinition = "jsonb", nullable = false)
    private String formDefinitionJson;

    @Column(name = "version")
    private Integer version = 1;

    @PrePersist
    void initPublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
}