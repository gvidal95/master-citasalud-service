package org.ups.citamedicos.integration.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.ups.citamedicos.adapter.out.persistence.FranjaHorariaJpaAdapter;
import org.ups.citamedicos.adapter.out.persistence.entity.FranjaHorariaEntity;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FranjaHorariaDisponibilidadAdapterTest {

    @Autowired
    private FranjaHorariaJpaRepository repository;

    private FranjaHorariaJpaAdapter adapter() {
        return new FranjaHorariaJpaAdapter(repository);
    }

    @Test
    void given_seededFranjas_when_findDisponibles_then_onlyLibreInDateRange() {
        FranjaHorariaJpaAdapter adapter = adapter();
        FranjaHorariaEntity first = repository.findAll().get(0);
        UUID medicoId = UUID.fromString(first.getMedicoId());

        List<FranjaHoraria> result = adapter.findByMedicoAndRangoFecha(
                medicoId, LocalDate.now(), LocalDate.now().plusDays(30), EstadoFranja.LIBRE);

        assertThat(result).allMatch(f -> f.getEstado() == EstadoFranja.LIBRE);
    }

    @Test
    void given_allFranjasOcupadas_when_findDisponibles_then_emptyList() {
        FranjaHorariaJpaAdapter adapter = adapter();
        FranjaHorariaEntity first = repository.findAll().get(0);
        UUID medicoId = UUID.fromString(first.getMedicoId());

        // Far future range where no seed data exists
        List<FranjaHoraria> result = adapter.findByMedicoAndRangoFecha(
                medicoId, LocalDate.now().plusDays(100), LocalDate.now().plusDays(130), EstadoFranja.LIBRE);

        assertThat(result).isEmpty();
    }
}
