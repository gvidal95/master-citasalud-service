package org.ups.citamedicos.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.ups.citamedicos.adapter.out.persistence.repository.CitaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.NotificacionJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ReservarCitaFuncionalTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private FranjaHorariaJpaRepository franjaHorariaJpaRepository;
    @Autowired private PacienteJpaRepository pacienteJpaRepository;
    @Autowired private CitaJpaRepository citaJpaRepository;
    @Autowired private NotificacionJpaRepository notificacionJpaRepository;

    @BeforeEach
    void resetDb() {
        notificacionJpaRepository.deleteAll();
        citaJpaRepository.deleteAll();
        franjaHorariaJpaRepository.findAll().forEach(f -> { f.setEstado("LIBRE"); franjaHorariaJpaRepository.save(f); });
    }

    @Test
    void given_seededFranjaLibre_when_postCitas_then_201_andFranjaNowOcupada() {
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        Map<String, Object> request = Map.of(
                "pacienteId", pacienteId,
                "franjaHorariaId", franjaId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/citas",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("estado");
        assertThat(response.getBody().get("estado")).isEqualTo("CONFIRMADA");

        String estadoFranja = franjaHorariaJpaRepository.findById(franjaId)
                .orElseThrow().getEstado();
        assertThat(estadoFranja).isEqualTo("OCUPADA");
    }

    @Test
    void given_sameIdempotencyKey_when_postCitasTwice_then_sameCodigo_and_noDuplicate() {
        String pacienteId = pacienteJpaRepository.findAll().get(1).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();
        String idemKey = UUID.randomUUID().toString();

        Map<String, Object> request = Map.of(
                "pacienteId", pacienteId,
                "franjaHorariaId", franjaId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idemKey);

        ResponseEntity<Map> first = restTemplate.exchange(
                "/api/v1/citas", HttpMethod.POST,
                new HttpEntity<>(request, headers), Map.class);
        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/citas", HttpMethod.POST,
                new HttpEntity<>(request, headers), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("codigo")).isEqualTo(second.getBody().get("codigo"));
    }
}
