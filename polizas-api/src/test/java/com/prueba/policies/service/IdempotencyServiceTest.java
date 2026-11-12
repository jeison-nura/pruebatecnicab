package com.prueba.policies.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.prueba.policies.domain.ApiIdempotency;
import com.prueba.policies.repository.ApiIdempotencyRepository;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {
    @Mock ApiIdempotencyRepository repository;
    @InjectMocks IdempotencyService service;

    @Test
    void newClaimReservesKeyBeforeExecution() {
        assertThat(service.claim("key", "hash", "POST", "/v1/riesgos").outcome())
                .isEqualTo(IdempotencyService.Outcome.CLAIMED);
        verify(repository).claim(eq("key"), eq("hash"), eq("POST"), eq("/v1/riesgos"), any(LocalDateTime.class));
    }

    @Test
    void duplicateWithSameHashReplaysCompletedResponse() {
        ApiIdempotency completed = new ApiIdempotency("key", "hash", "POST", "/path", 201,
                "{\"ok\":true}", LocalDateTime.now().plusHours(1));
        when(repository.claim(eq("key"), eq("hash"), eq("POST"), eq("/path"), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findById("key")).thenReturn(Optional.of(completed));

        var claim = service.claim("key", "hash", "POST", "/path");
        assertThat(claim.outcome()).isEqualTo(IdempotencyService.Outcome.REPLAY);
        assertThat(claim.response()).isSameAs(completed);
    }

    @Test
    void duplicateWithDifferentHashIsConflict() {
        when(repository.claim(eq("key"), eq("new"), eq("POST"), eq("/path"), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findById("key")).thenReturn(Optional.of(new ApiIdempotency(
                "key", "original", "POST", "/path", 200, "{}", LocalDateTime.now().plusHours(1))));

        assertThat(service.claim("key", "new", "POST", "/path").outcome())
                .isEqualTo(IdempotencyService.Outcome.CONFLICT);
    }

    @Test
    void lostClaimMustNotSilentlyPretendToStoreResponse() {
        when(repository.complete("key", "hash", 200, "{}" )).thenReturn(0);
        assertThatThrownBy(() -> service.complete("key", "hash", 200, "{}"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("lost");
    }
}
