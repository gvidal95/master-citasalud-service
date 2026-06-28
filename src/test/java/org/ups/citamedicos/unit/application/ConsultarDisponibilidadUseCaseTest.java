package org.ups.citamedicos.unit.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase.ConsultarDisponibilidadQuery;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.application.usecase.ConsultarDisponibilidadUseCaseImpl;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarDisponibilidadUseCaseTest {

    @Mock
    private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;

    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;

    @InjectMocks
    private ConsultarDisponibilidadUseCaseImpl useCase;

    @Test
    void given_medicoWithFranjasLibres_when_execute_then_returnOnlyLibres() {
        UUID medicoId = UUID.randomUUID();
        FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId,
                LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(9, 30),
                EstadoFranja.LIBRE, 0L);
        when(medicoRepositoryPort.findById(medicoId))
                .thenReturn(Optional.of(new Medico(medicoId, "Dr.", "Cardio")));
        when(franjaHorariaRepositoryPort.findByMedicoAndRangoFecha(any(), any(), any(), any()))
                .thenReturn(List.of(franja));

        ConsultarDisponibilidadQuery query = new ConsultarDisponibilidadQuery(
                medicoId, LocalDate.now(), LocalDate.now().plusDays(7), EstadoFranja.LIBRE);
        List<FranjaHoraria> result = useCase.execute(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void given_rangoMayorA60Dias_when_execute_then_throwsIllegalArgumentException() {
        UUID medicoId = UUID.randomUUID();
        ConsultarDisponibilidadQuery query = new ConsultarDisponibilidadQuery(
                medicoId, LocalDate.now(), LocalDate.now().plusDays(61), EstadoFranja.LIBRE);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
