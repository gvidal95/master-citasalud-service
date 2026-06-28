package org.ups.citamedicos.integration.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.ups.citamedicos.adapter.out.persistence.MedicoJpaAdapter;
import org.ups.citamedicos.adapter.out.persistence.repository.MedicoJpaRepository;
import org.ups.citamedicos.domain.model.Medico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MedicoJpaAdapterTest {

    @Autowired
    private MedicoJpaRepository repository;

    private MedicoJpaAdapter adapter() {
        return new MedicoJpaAdapter(repository);
    }

    @Test
    void given_seededMedicos_when_findByEspecialidad_then_filtered() {
        MedicoJpaAdapter adapter = adapter();
        List<Medico> result = adapter.findByEspecialidadAndNombre("Cardiología", null);
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(m -> m.getEspecialidad().contains("Cardiología"));
    }

    @Test
    void given_seededMedicos_when_findAll_then_returnsThree() {
        MedicoJpaAdapter adapter = adapter();
        List<Medico> result = adapter.findByEspecialidadAndNombre(null, null);
        assertThat(result).hasSize(3);
    }

    @Test
    void given_seededMedicos_when_findByNombreOnly_then_filtered() {
        MedicoJpaAdapter adapter = adapter();
        List<Medico> result = adapter.findByEspecialidadAndNombre(null, "Ana");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(m -> m.getNombre().contains("Ana"));
    }

    @Test
    void given_seededMedicos_when_findByBothFilters_then_filtered() {
        MedicoJpaAdapter adapter = adapter();
        List<Medico> result = adapter.findByEspecialidadAndNombre("Cardiología", "Ana");
        assertThat(result).isNotEmpty();
    }

    @Test
    void given_seededMedico_when_findById_then_found() {
        MedicoJpaAdapter adapter = adapter();
        List<Medico> all = adapter.findByEspecialidadAndNombre(null, null);
        UUID id = all.get(0).getId();
        Optional<Medico> found = adapter.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    void given_unknownId_when_findById_then_empty() {
        MedicoJpaAdapter adapter = adapter();
        Optional<Medico> result = adapter.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}
