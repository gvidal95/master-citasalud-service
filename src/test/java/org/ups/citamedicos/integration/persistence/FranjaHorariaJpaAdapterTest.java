package org.ups.citamedicos.integration.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.ups.citamedicos.adapter.out.persistence.FranjaHorariaJpaAdapter;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FranjaHorariaJpaAdapterTest {

    @Autowired
    private FranjaHorariaJpaRepository repository;

    private FranjaHorariaJpaAdapter adapter() {
        return new FranjaHorariaJpaAdapter(repository);
    }

    @Test
    void given_seededFranjaLibre_when_findById_then_returnsLibre() {
        FranjaHorariaJpaAdapter adapter = adapter();
        String franjaId = repository.findAll().get(0).getId();

        Optional<FranjaHoraria> franja = adapter.findById(UUID.fromString(franjaId));

        assertThat(franja).isPresent();
        assertThat(franja.get().getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void given_franjaReserved_when_save_then_estadoOcupada() {
        FranjaHorariaJpaAdapter adapter = adapter();
        String franjaId = repository.findAll().get(0).getId();

        FranjaHoraria franja = adapter.findById(UUID.fromString(franjaId)).orElseThrow();
        franja.reservar();
        FranjaHoraria saved = adapter.save(franja);

        assertThat(saved.getEstado()).isEqualTo(EstadoFranja.OCUPADA);
    }
}
