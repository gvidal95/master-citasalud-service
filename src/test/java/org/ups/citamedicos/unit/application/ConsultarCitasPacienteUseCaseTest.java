package org.ups.citamedicos.unit.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase.ConsultarCitasPacienteQuery;
import org.ups.citamedicos.application.port.out.CitaRepositoryPort;
import org.ups.citamedicos.application.port.out.PacienteRepositoryPort;
import org.ups.citamedicos.application.usecase.ConsultarCitasPacienteUseCaseImpl;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.Paciente;
import org.ups.citamedicos.domain.valueobject.NumeroWhatsApp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarCitasPacienteUseCaseTest {

    @Mock
    private CitaRepositoryPort citaRepositoryPort;

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;

    @InjectMocks
    private ConsultarCitasPacienteUseCaseImpl useCase;

    @Test
    void given_pacienteWithCitas_when_execute_then_returnConfirmadasSortedByFecha() {
        UUID pacienteId = UUID.randomUUID();
        Paciente paciente = new Paciente(pacienteId, "Juan", NumeroWhatsApp.of("+593987654321"));
        Cita cita = Cita.nueva(pacienteId, UUID.randomUUID(), "idem-1",
                LocalDateTime.now().plusDays(5));

        when(pacienteRepositoryPort.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(citaRepositoryPort.findConfirmadasByPacienteId(pacienteId)).thenReturn(List.of(cita));

        List<Cita> result = useCase.execute(new ConsultarCitasPacienteQuery(pacienteId));

        assertThat(result).hasSize(1);
    }

    @Test
    void given_pacienteNotFound_when_execute_then_throwsNoSuchElement() {
        UUID pacienteId = UUID.randomUUID();
        when(pacienteRepositoryPort.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ConsultarCitasPacienteQuery(pacienteId)))
                .isInstanceOf(NoSuchElementException.class);
    }
}
