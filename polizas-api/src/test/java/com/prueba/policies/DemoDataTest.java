package com.prueba.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.prueba.policies.repository.PolicyJpaRepository;
import com.prueba.policies.repository.RiskJpaRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:policies-demo-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class DemoDataTest {

    private static final UUID GROUP_ID = UUID.fromString("01a1ce48-d512-720a-97c8-386d6a328fdf");
    private static final UUID INDIVIDUAL_ID = UUID.fromString("01a1ce48-d515-7548-93ed-b7daeed7900f");

    @Autowired MockMvc mvc;
    @Autowired PolicyJpaRepository policies;
    @Autowired RiskJpaRepository risks;

    @Test
    void demoProfileStartsWithBothPolicyTypesAndUsableRisks() throws Exception {
        assertThat(policies.findById(GROUP_ID)).isPresent();
        assertThat(policies.findById(INDIVIDUAL_ID)).isPresent();
        assertThat(risks.findByPolicyId(GROUP_ID)).hasSize(2);
        assertThat(risks.findByPolicyId(INDIVIDUAL_ID)).hasSize(1);

        mvc.perform(get("/v1/polizas").param("tipo", "GROUP").header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("GROUP"));
        mvc.perform(get("/v1/polizas/{id}/riesgos", GROUP_ID).header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void demoIpcAppliesToRenewal() throws Exception {
        mvc.perform(post("/v1/polizas/{id}/renovar", GROUP_ID).header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENEWED"))
                .andExpect(jsonPath("$.monthlyRent").value("1242.00"))
                .andExpect(jsonPath("$.premium").value("7452.00"));
    }
}
