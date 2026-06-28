package org.ups.citamedicos.integration.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.ups.citamedicos.adapter.out.persistence.CitaJpaAdapter;
import org.ups.citamedicos.adapter.out.persistence.repository.CitaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.MedicoJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.valueobject.CodigoCita;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CitaJpaAdapterTest {

    @Autowired
    private CitaJpaRepository citaJpaRepository;
    @Autowired
    private PacienteJpaRepository pacienteJpaRepository;
    @Autowired
    private MedicoJpaRepository medicoJpaRepository;
    @Autowired
    private FranjaHorariaJpaRepository franjaHorariaJpaRepository;

    private CitaJpaAdapter adapter() {
        return new CitaJpaAdapter(citaJpaRepository, pacienteJpaRepository, medicoJpaRepository, franjaHorariaJpaRepository);
    }

    @Test
    void given_seededMedicoAndPaciente_when_saveCita_then_persisted() {
        CitaJpaAdapter adapter = adapter();

        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().get(0).getId();

        Cita cita = Cita.nueva(UUID.fromString(pacienteId), UUID.fromString(franjaId),
                UUID.randomUUID().toString(), LocalDateTime.now().plusDays(5));
        Cita saved = adapter.save(cita);

        assertThat(saved).isNotNull();
        assertThat(saved.getCodigo()).isNotNull();
    }

    @Test
    void given_citaSaved_when_findByCodigo_then_returnsNonEmpty() {
        CitaJpaAdapter adapter = adapter();
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().get(0).getId();

        Cita cita = Cita.nueva(UUID.fromString(pacienteId), UUID.fromString(franjaId),
                UUID.randomUUID().toString(), LocalDateTime.now().plusDays(5));
        Cita saved = adapter.save(cita);

        Optional<Cita> found = adapter.findByCodigo(saved.getCodigo());
        assertThat(found).isPresent();
    }

    @Test
    void given_idempotencyKey_when_findByIdempotencyKey_then_returnsNonEmpty() {
        CitaJpaAdapter adapter = adapter();
        String pacienteId = pacienteJpaRepository.findAll().get(0).getId();
        String franjaId = franjaHorariaJpaRepository.findAll().get(0).getId();
        String idemKey = UUID.randomUUID().toString();

        Cita cita = Cita.nueva(UUID.fromString(pacienteId), UUID.fromString(franjaId),
                idemKey, LocalDateTime.now().plusDays(5));
        adapter.save(cita);

        Optional<Cita> found = adapter.findByIdempotencyKey(idemKey);
        assertThat(found).isPresent();
    }
}
