package com.prueba.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A lease insurance policy. Independent entity (no JPA inheritance): the type
 * references the PolicyType catalog (FK) and the type-specific rules
 * (1 risk for INDIVIDUAL, N for GROUP) live in the service layer, not in a
 * class hierarchy.
 */
@Entity
@Table(name = "policy", schema = "policy")
public class Policy {

    /**
     * UUIDv7 (time-ordered, RFC 9562): non-guessable through the API,
     * generatable on any node without DB coordination (supports the future
     * extraction to microservices), with index locality close to a sequential id.
     */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_type_id", nullable = false)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column(name = "validity_start", nullable = false)
    private LocalDate validityStart;

    @Column(name = "validity_end", nullable = false)
    private LocalDate validityEnd;

    @Column(name = "initial_months", nullable = false)
    private long initialMonths;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    /** premium = monthly rent x months of validity */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal premium;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Policy() {
        // required by JPA
    }

    public Policy(PolicyType policyType, LocalDate validityStart, LocalDate validityEnd, BigDecimal monthlyRent) {
        this.policyType = policyType;
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
        this.monthlyRent = monthlyRent;
        this.initialMonths = monthsOfValidity();
        this.premium = monthlyRent.multiply(BigDecimal.valueOf(initialMonths));
    }

    public long monthsOfValidity() {
        return Math.max(1, ChronoUnit.MONTHS.between(validityStart, validityEnd));
    }

    public long getInitialMonths() {
        return initialMonths;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public LocalDate getValidityStart() {
        return validityStart;
    }

    public LocalDate getValidityEnd() {
        return validityEnd;
    }

    public void setValidityEnd(LocalDate validityEnd) {
        this.validityEnd = validityEnd;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
