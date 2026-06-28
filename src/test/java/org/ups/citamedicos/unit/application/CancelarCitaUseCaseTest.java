package org.ups.citamedicos.unit.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase.CancelarCitaCommand;
import org.ups.citamedicos.application.port.out.*;
import org.ups.citamedicos.application.usecase.CancelarCitaUseCaseImpl;
import org.ups.citamedicos.domain.exception.CancelacionFueraDePlazoException;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Notificacion;
import org.ups.citamedicos.domain.valueobject.CodigoCita;
import org.ups.citamedicos.domain.valueobject.EstadoCita;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelarCitaUseCaseTest {

    @Mock private CitaRepositoryPort citaRepositoryPort;
    @Mock private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @Mock private NotificacionWhatsAppPort notificacionWhatsAppPort;
    @Mock private NotificacionRepositoryPort notificacionRepositoryPort;
    @Mock private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock private MedicoRepositoryPort medicoRepositoryPort;

    @InjectMocks
    private CancelarCitaUseCaseImpl useCase;

    @Test
    void given_citaConfirmadaWith25hAntelacion_when_execute_then_estadoCancelada_andFranjaLibre() {
        UUID franjaId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        LocalDateTime franjaInicio = LocalDateTime.now(ZoneOffset.UTC).plusHours(25);
        CodigoCita codigo = CodigoCita.generate();

        Cita cita = Cita.reconstituir(codigo, UUID.randomUUID(), franjaId,
                "idem-key", java.time.Instant.now(), franjaInicio, EstadoCita.CONFIRMADA);
        FranjaHoraria franja = new FranjaHoraria(franjaId, medicoId,
                LocalDate.now().plusDays(2), LocalTime.of(10, 0), LocalTime.of(10, 30),
                EstadoFranja.OCUPADA, 1L);

        when(citaRepositoryPort.findByCodigoForUpdate(codigo)).thenReturn(Optional.of(cita));
        when(citaRepositoryPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franja));
        when(franjaHorariaRepositoryPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pacienteRepositoryPort.findById(any())).thenReturn(Optional.empty());

        useCase.execute(new CancelarCitaCommand(codigo));

        verify(citaRepositoryPort).save(any());
        verify(franjaHorariaRepositoryPort).save(any());
    }

    @Test
    void given_citaConfirmadaWith12h_when_execute_then_throwsCancelacionFueraDePlazoException() {
        UUID franjaId = UUID.randomUUID();
        LocalDateTime franjaInicio = LocalDateTime.now(ZoneOffset.UTC).plusHours(12);
        CodigoCita codigo = CodigoCita.generate();

        Cita cita = Cita.reconstituir(codigo, UUID.randomUUID(), franjaId,
                "idem-key", java.time.Instant.now(), franjaInicio, EstadoCita.CONFIRMADA);

        when(citaRepositoryPort.findByCodigoForUpdate(codigo)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> useCase.execute(new CancelarCitaCommand(codigo)))
                .isInstanceOf(CancelacionFueraDePlazoException.class);
    }

    @Test
    void given_citaYaCancelada_when_execute_then_throwsIllegalState() {
        UUID franjaId = UUID.randomUUID();
        LocalDateTime franjaInicio = LocalDateTime.now(ZoneOffset.UTC).plusHours(48);
        CodigoCita codigo = CodigoCita.generate();

        Cita cita = Cita.reconstituir(codigo, UUID.randomUUID(), franjaId,
                "idem-key", java.time.Instant.now(), franjaInicio, EstadoCita.CANCELADA);

        when(citaRepositoryPort.findByCodigoForUpdate(codigo)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> useCase.execute(new CancelarCitaCommand(codigo)))
                .isInstanceOf(IllegalStateException.class);
    }
}
