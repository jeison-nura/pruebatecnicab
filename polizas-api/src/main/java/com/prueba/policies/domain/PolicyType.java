package com.prueba.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Policy type catalog (table policy_type): 1 = INDIVIDUAL, 2 = GROUP.
 * Static catalog: ids are fixed, not generated.
 */
@Entity
@Table(name = "policy_type", schema = "policy")
public class PolicyType {

    public static final String INDIVIDUAL = "INDIVIDUAL";
    public static final String GROUP = "GROUP";

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column
    private String description;

    protected PolicyType() {
        // required by JPA
    }

    public PolicyType(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public boolean isIndividual() {
        return INDIVIDUAL.equals(name);
    }

    public boolean isGroup() {
        return GROUP.equals(name);
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
