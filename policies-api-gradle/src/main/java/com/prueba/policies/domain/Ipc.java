package com.prueba.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** IPC (Consumer Price Index) — percentage with application date. */
@Entity
@Table(name = "ipc", schema = "policy")
public class Ipc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_date", nullable = false, unique = true)
    private LocalDate applicationDate;

    @Column(name = "percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal percentage;

    protected Ipc() {
        // required by JPA
    }

    public Ipc(LocalDate applicationDate, BigDecimal percentage) {
        this.applicationDate = applicationDate;
        this.percentage = percentage;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}