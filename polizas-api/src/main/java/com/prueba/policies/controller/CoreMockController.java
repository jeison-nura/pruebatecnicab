package com.prueba.policies.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    public ResponseEntity<Void> receiveEvent(@Valid @RequestBody CoreEvent payload) {
        log.info("CORE send attempted for policy {} (event {})", payload.polizaId(), payload.evento());
        return ResponseEntity.ok().build();
    }

    public record CoreEvent(@NotBlank String evento, @NotNull Long polizaId) {}
}
