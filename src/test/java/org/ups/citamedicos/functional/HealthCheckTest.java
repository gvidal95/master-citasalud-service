package org.ups.citamedicos.functional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-001: El sistema debe estar disponible 24/7.
 * Verifica que el endpoint de health responde UP, condición necesaria para monitoreo externo de disponibilidad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class HealthCheckTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void given_applicationStarted_when_getHealth_then_200_statusUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }
}
