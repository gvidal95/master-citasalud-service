package org.ups.citamedicos.unit.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase.ReservarCitaCommand;
import org.ups.citamedicos.application.port.out.*;
import org.ups.citamedicos.application.usecase.ReservarCitaUseCaseImpl;
import org.ups.citamedicos.domain.exception.CitaDuplicadaException;
import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.model.*;
import org.ups.citamedicos.domain.valueobject.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservarCitaUseCaseTest {

    @Mock private CitaRepositoryPort citaRepositoryPort;
    @Mock private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @Mock private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock private MedicoRepositoryPort medicoRepositoryPort;
    @Mock private NotificacionWhatsAppPort notificacionWhatsAppPort;
    @Mock private NotificacionRepositoryPort notificacionRepositoryPort;

    @InjectMocks
    private ReservarCitaUseCaseImpl useCase;

    private UUID pacienteId;
    private UUID medicoId;
    private UUID franjaId;
    private FranjaHoraria franjaLibre;
    private Paciente paciente;
    private Medico medico;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
        franjaId = UUID.randomUUID();

        franjaLibre = new FranjaHoraria(franjaId, medicoId,
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                EstadoFranja.LIBRE, 0L);

        paciente = new Paciente(pacienteId, "Juan Pérez", NumeroWhatsApp.of("+593987654321"));
        medico = new Medico(medicoId, "Dr. García", "Cardiología");
    }

    @Test
    void given_franjaLibre_when_execute_then_citaSavedAndNotificacionDispatched() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(citaRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franjaLibre));
        when(citaRepositoryPort.existsConfirmadaByPacienteAndMedicoAndFecha(
                eq(pacienteId), eq(medicoId), any(LocalDate.class))).thenReturn(false);
        when(pacienteRepositoryPort.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(medicoRepositoryPort.findById(medicoId)).thenReturn(Optional.of(medico));
        when(citaRepositoryPort.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
        when(franjaHorariaRepositoryPort.save(any(FranjaHoraria.class))).thenAnswer(i -> i.getArgument(0));
        when(notificacionRepositoryPort.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Cita result = useCase.execute(new ReservarCitaCommand(pacienteId, franjaId, idempotencyKey));

        assertThat(result.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        verify(citaRepositoryPort).save(any(Cita.class));
        verify(franjaHorariaRepositoryPort).save(any(FranjaHoraria.class));
    }

    @Test
    void given_idempotencyKeyExists_when_execute_then_returnExistingCita() {
        String idempotencyKey = "existing-key";
        Cita existingCita = Cita.nueva(pacienteId, franjaId, idempotencyKey,
                LocalDateTime.now().plusDays(5));
        when(citaRepositoryPort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingCita));

        Cita result = useCase.execute(new ReservarCitaCommand(pacienteId, franjaId, idempotencyKey));

        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        verify(citaRepositoryPort, never()).save(any());
    }

    @Test
    void given_pacienteAlreadyHasCitaSameDia_when_execute_then_throwsCitaDuplicadaException() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(citaRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franjaLibre));
        when(citaRepositoryPort.existsConfirmadaByPacienteAndMedicoAndFecha(
                eq(pacienteId), eq(medicoId), any(LocalDate.class))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ReservarCitaCommand(pacienteId, franjaId, idempotencyKey)))
                .isInstanceOf(CitaDuplicadaException.class);
    }

    @Test
    void given_franjaOcupada_when_execute_then_throwsFranjaNoDisponibleException() {
        String idempotencyKey = UUID.randomUUID().toString();
        FranjaHoraria franjaOcupada = new FranjaHoraria(franjaId, medicoId,
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                EstadoFranja.OCUPADA, 1L);
        when(citaRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franjaOcupada));
        when(citaRepositoryPort.existsConfirmadaByPacienteAndMedicoAndFecha(
                eq(pacienteId), eq(medicoId), any(LocalDate.class))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ReservarCitaCommand(pacienteId, franjaId, idempotencyKey)))
                .isInstanceOf(FranjaNoDisponibleException.class);
    }
}
