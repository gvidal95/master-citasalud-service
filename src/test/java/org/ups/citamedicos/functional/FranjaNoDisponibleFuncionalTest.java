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
import org.ups.citamedicos.adapter.out.persistence.repository.MedicoJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.NotificacionJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class FranjaNoDisponibleFuncionalTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private FranjaHorariaJpaRepository franjaHorariaJpaRepository;
    @Autowired private MedicoJpaRepository medicoJpaRepository;
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
    void given_seededFranjaLibre_when_book_then_201() {
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        ResponseEntity<Map> response = postCita(pacienteId, franjaId, UUID.randomUUID().toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void given_sameFranja_when_secondBook_then_409() {
        String pacienteId1 = pacienteJpaRepository.findAll().get(0).getId();
        String pacienteId2 = pacienteJpaRepository.findAll().get(1).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().stream()
                .filter(f -> "LIBRE".equals(f.getEstado()))
                .findFirst()
                .orElseThrow()
                .getId();

        postCita(pacienteId1, franjaId, UUID.randomUUID().toString());
        ResponseEntity<Map> second = postCita(pacienteId2, franjaId, UUID.randomUUID().toString());

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void given_seededMedico_when_getDisponibilidad_then_returnsOnlyLibreFranjas() {
        String medicoId = medicoJpaRepository.findAll().get(0).getId();

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/medicos/{medicoId}/disponibilidad?fechaDesde={desde}&fechaHasta={hasta}",
                List.class, medicoId, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map> postCita(String pacienteId, String franjaId, String idemKey) {
        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idemKey);
        return restTemplate.exchange("/api/v1/citas", HttpMethod.POST,
                new HttpEntity<>(request, headers), Map.class);
    }
}
