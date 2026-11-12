package com.prueba.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.Risk;
import com.prueba.policies.domain.RiskStatus;
import com.prueba.policies.repository.ApiIdempotencyRepository;
import com.prueba.policies.repository.PolicyJpaRepository;
import com.prueba.policies.repository.PolicyTypeJpaRepository;
import com.prueba.policies.repository.RiskJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyApiTest {

    @Autowired MockMvc mvc;
    @Autowired PolicyJpaRepository policies;
    @Autowired PolicyTypeJpaRepository types;
    @Autowired RiskJpaRepository risks;
    @Autowired ApiIdempotencyRepository idempotency;

    @AfterEach
    void cleanUp() {
        idempotency.deleteAll();
        risks.deleteAll();
        policies.deleteAll();
    }

    @Test
    void rejectsRequestsWithoutApiKey() throws Exception {
        mvc.perform(get("/v1/polizas")).andExpect(status().isUnauthorized());
    }

    @Test
    void cancelsRiskAtRequiredRoute() throws Exception {
        Policy policy = policies.save(new Policy(types.findById(2).orElseThrow(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        Risk risk = risks.save(new Risk(policy, "House"));

        mvc.perform(post("/v1/riesgos/{id}/cancelar", risk.getId()).header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsBlankRiskDescriptionAndIndividualPolicy() throws Exception {
        UUID id = policies.save(new Policy(types.findById(1).orElseThrow(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), new BigDecimal("1000"))).getId();

        mvc.perform(post("/v1/polizas/{id}/riesgos", id).header("x-api-key", "123456")
                        .contentType("application/json").content("{\"description\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/polizas/{id}/riesgos", id).header("x-api-key", "123456")
                        .contentType("application/json").content("{\"description\":\"House\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listsUnknownPolicyRisksAsNotFound() throws Exception {
        mvc.perform(get("/v1/polizas/{id}/riesgos", UUID.randomUUID()).header("x-api-key", "123456"))
                .andExpect(status().isNotFound());
    }

    @Test
    void coreMockAcceptsContractWithoutEchoingPayload() throws Exception {
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .contentType("application/json")
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsReusedIdempotencyKeyForDifferentRequests() throws Exception {
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", "test-reused-key")
                        .contentType("application/json")
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isOk());
        assertThat(idempotency.findById("test-reused-key")).isPresent();
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", "test-reused-key")
                        .contentType("application/json")
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":556}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cachedResponseStillRequiresApiKey() throws Exception {
        String payload = "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}";
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", "secure-replay")
                        .contentType("application/json").content(payload))
                .andExpect(status().isOk());
        mvc.perform(post("/core-mock/evento").header("Idempotency-Key", "secure-replay")
                        .contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", "secure-replay")
                        .contentType("application/json").content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void cancellingPolicyPersistsCancellationOfAllItsRisks() throws Exception {
        Policy policy = policies.save(new Policy(types.findById(2).orElseThrow(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        Risk first = risks.save(new Risk(policy, "Apartment 1"));
        Risk second = risks.save(new Risk(policy, "Apartment 2"));

        mvc.perform(post("/v1/polizas/{id}/cancelar", policy.getId()).header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(risks.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(RiskStatus.CANCELLED);
        assertThat(risks.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(RiskStatus.CANCELLED);
    }

    @Test
    void listsPoliciesWithTypeAndStatusFilters() throws Exception {
        policies.save(new Policy(types.findById(2).orElseThrow(), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        mvc.perform(get("/v1/polizas").param("tipo", "GROUP").param("estado", "ACTIVE")
                        .header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].type").value("GROUP"));
    }

    @Test
    void listsRisksOfExistingPolicy() throws Exception {
        Policy policy = policies.save(new Policy(types.findById(2).orElseThrow(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        risks.save(new Risk(policy, "Apartment 1"));

        mvc.perform(get("/v1/polizas/{id}/riesgos", policy.getId()).header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].description").value("Apartment 1"));
    }

    @Test
    void paginatesPoliciesWithFiltersAndMetadata() throws Exception {
        policies.save(new Policy(types.findById(2).orElseThrow(), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        policies.save(new Policy(types.findById(2).orElseThrow(), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1), new BigDecimal("2000")));
        policies.save(new Policy(types.findById(1).orElseThrow(), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1), new BigDecimal("3000")));

        mvc.perform(get("/v1/polizas").param("tipo", "GROUP").param("estado", "ACTIVE")
                        .param("page", "1").param("size", "1").header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("GROUP"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void paginatesRisksWithinPolicy() throws Exception {
        Policy policy = policies.save(new Policy(types.findById(2).orElseThrow(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), new BigDecimal("1000")));
        risks.save(new Risk(policy, "Apartment 1"));
        risks.save(new Risk(policy, "Apartment 2"));

        mvc.perform(get("/v1/polizas/{id}/riesgos", policy.getId())
                        .param("page", "1").param("size", "1").header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].policyId").value(policy.getId().toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/v1/polizas/{id}/riesgos", policy.getId())
                        .param("page", "2").param("size", "1").header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        for (String[] params : new String[][] { { "-1", "20" }, { "0", "0" }, { "0", "101" }, { "abc", "20" } }) {
            mvc.perform(get("/v1/polizas").param("page", params[0]).param("size", params[1])
                            .header("x-api-key", "123456"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/v1/polizas/{id}/riesgos", UUID.randomUUID())
                            .param("page", params[0]).param("size", params[1])
                            .header("x-api-key", "123456"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void defaultsToBoundedFirstPage() throws Exception {
        mvc.perform(get("/v1/polizas").header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
