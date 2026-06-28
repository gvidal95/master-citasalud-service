package org.ups.citamedicos.application.usecase;

import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.out.*;
import org.ups.citamedicos.domain.exception.CitaDuplicadaException;
import org.ups.citamedicos.domain.model.*;
import org.ups.citamedicos.domain.valueobject.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class ReservarCitaUseCaseImpl implements ReservarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;
    private final NotificacionWhatsAppPort notificacionWhatsAppPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;

    public ReservarCitaUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                   FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                   PacienteRepositoryPort pacienteRepositoryPort,
                                   MedicoRepositoryPort medicoRepositoryPort,
                                   NotificacionWhatsAppPort notificacionWhatsAppPort,
                                   NotificacionRepositoryPort notificacionRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.pacienteRepositoryPort = pacienteRepositoryPort;
        this.medicoRepositoryPort = medicoRepositoryPort;
        this.notificacionWhatsAppPort = notificacionWhatsAppPort;
        this.notificacionRepositoryPort = notificacionRepositoryPort;
    }

    @Override
    public Cita execute(ReservarCitaCommand command) {
        Optional<Cita> existing = citaRepositoryPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        FranjaHoraria franja = franjaHorariaRepositoryPort.findById(command.franjaHorariaId())
                .orElseThrow(() -> new NoSuchElementException("Franja no encontrada: " + command.franjaHorariaId()));

        boolean duplicate = citaRepositoryPort.existsConfirmadaByPacienteAndMedicoAndFecha(
                command.pacienteId(), franja.getMedicoId(), franja.getFecha());
        if (duplicate) {
            throw new CitaDuplicadaException(command.idempotencyKey());
        }

        franja.reservar();

        LocalDateTime franjaInicio = LocalDateTime.of(franja.getFecha(), franja.getHoraInicio());
        Cita cita = Cita.nueva(command.pacienteId(), command.franjaHorariaId(),
                command.idempotencyKey(), franjaInicio);

        Cita savedCita = citaRepositoryPort.save(cita);
        franjaHorariaRepositoryPort.save(franja);

        dispatchNotificacion(savedCita, command.pacienteId(), franja.getMedicoId());

        return savedCita;
    }

    private void dispatchNotificacion(Cita cita, UUID pacienteId, UUID medicoId) {
        Paciente paciente = pacienteRepositoryPort.findById(pacienteId)
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado: " + pacienteId));
        Medico medico = medicoRepositoryPort.findById(medicoId)
                .orElseThrow(() -> new NoSuchElementException("Médico no encontrado: " + medicoId));

        UUID citaUuid = UUID.fromString(cita.getCodigo().getValue());
        Notificacion notificacion = Notificacion.nueva(citaUuid, TipoNotificacion.CONFIRMACION);
        Notificacion savedNotificacion = notificacionRepositoryPort.save(notificacion);
        notificacionWhatsAppPort.enviar(savedNotificacion, paciente, cita, medico);
    }
}
