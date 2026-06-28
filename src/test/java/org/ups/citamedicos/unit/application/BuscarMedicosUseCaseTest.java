package org.ups.citamedicos.unit.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase.BuscarMedicosQuery;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.application.usecase.BuscarMedicosUseCaseImpl;
import org.ups.citamedicos.domain.model.Medico;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarMedicosUseCaseTest {

    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;

    @InjectMocks
    private BuscarMedicosUseCaseImpl useCase;

    @Test
    void given_especialidadFilter_when_execute_then_returnMatchingMedicos() {
        Medico medico = new Medico(UUID.randomUUID(), "Dr. García", "Cardiología");
        when(medicoRepositoryPort.findByEspecialidadAndNombre("Cardiología", null))
                .thenReturn(List.of(medico));

        List<Medico> result = useCase.execute(new BuscarMedicosQuery("Cardiología", null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEspecialidad()).isEqualTo("Cardiología");
    }

    @Test
    void given_noFilter_when_execute_then_returnAll() {
        List<Medico> all = List.of(
                new Medico(UUID.randomUUID(), "Dr. A", "Cardiología"),
                new Medico(UUID.randomUUID(), "Dr. B", "Pediatría")
        );
        when(medicoRepositoryPort.findByEspecialidadAndNombre(null, null)).thenReturn(all);

        List<Medico> result = useCase.execute(new BuscarMedicosQuery(null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void given_emptyResult_when_execute_then_returnEmptyList() {
        when(medicoRepositoryPort.findByEspecialidadAndNombre("Inexistente", null))
                .thenReturn(List.of());

        List<Medico> result = useCase.execute(new BuscarMedicosQuery("Inexistente", null));

        assertThat(result).isEmpty();
    }
}
