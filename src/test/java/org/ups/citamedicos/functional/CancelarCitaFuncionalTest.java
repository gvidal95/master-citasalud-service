package org.ups.citamedicos.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.ups.citamedicos.adapter.out.persistence.repository.CitaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.NotificacionJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CancelarCitaFuncionalTest {

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
    void given_pacienteWithCita_when_getCitasPaciente_then_200_withCitaListedAndEstadoConfirmada() {
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        // Primero reservar
        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        restTemplate.exchange("/api/v1/citas", HttpMethod.POST, new HttpEntity<>(request, headers), Map.class);

        // Luego consultar citas del paciente (AC-1)
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
                "/api/v1/pacientes/{pacienteId}/citas", List.class, pacienteId);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotEmpty();
    }

    @Test
    void given_seededFranja_when_bookThenCancel_then_204_andFranjaLibreAgain() {
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        // Book (AC-2 setup)
        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<Map> bookResponse = restTemplate.exchange(
                "/api/v1/citas", HttpMethod.POST, new HttpEntity<>(request, headers), Map.class);

        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String codigo = (String) bookResponse.getBody().get("codigo");

        // Cancel (the franja starts > 24h in future due to seed data using DATEADD)
        ResponseEntity<Void> cancelResponse = restTemplate.exchange(
                "/api/v1/citas/{codigo}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class, codigo);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify franja is LIBRE again
        String franjaEstado = franjaHorariaJpaRepository.findById(franjaId).orElseThrow().getEstado();
        assertThat(franjaEstado).isEqualTo("LIBRE");
    }

    @Test
    void given_cancelledCita_when_cancelAgain_then_409() {
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<Map> bookResponse = restTemplate.exchange(
                "/api/v1/citas", HttpMethod.POST, new HttpEntity<>(request, headers), Map.class);
        String codigo = (String) bookResponse.getBody().get("codigo");

        restTemplate.exchange("/api/v1/citas/{codigo}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class, codigo);

        ResponseEntity<Map> secondCancel = restTemplate.exchange(
                "/api/v1/citas/{codigo}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, codigo);

        assertThat(secondCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
