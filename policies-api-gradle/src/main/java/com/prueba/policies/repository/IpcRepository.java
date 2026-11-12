package com.prueba.policies.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prueba.policies.domain.Ipc;

public interface IpcRepository extends JpaRepository<Ipc, Long> {

    /**
     * Returns the latest IPC with application_date <= given date.
     * Used to adjust the canon on renewal.
     */
    Optional<Ipc> findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(LocalDate date);
}