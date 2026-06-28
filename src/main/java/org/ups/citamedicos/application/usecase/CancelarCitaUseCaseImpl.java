package org.ups.citamedicos.application.usecase;

import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.out.*;
import org.ups.citamedicos.domain.model.*;
import org.ups.citamedicos.domain.valueobject.TipoNotificacion;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

public class CancelarCitaUseCaseImpl implements CancelarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final NotificacionWhatsAppPort notificacionWhatsAppPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;

    public CancelarCitaUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                   FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                   NotificacionWhatsAppPort notificacionWhatsAppPort,
                                   NotificacionRepositoryPort notificacionRepositoryPort,
                                   PacienteRepositoryPort pacienteRepositoryPort,
                                   MedicoRepositoryPort medicoRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.notificacionWhatsAppPort = notificacionWhatsAppPort;
        this.notificacionRepositoryPort = notificacionRepositoryPort;
        this.pacienteRepositoryPort = pacienteRepositoryPort;
        this.medicoRepositoryPort = medicoRepositoryPort;
    }

    @Override
    public void execute(CancelarCitaCommand command) {
        Cita cita = citaRepositoryPort.findByCodigoForUpdate(command.codigoCita())
                .orElseThrow(() -> new NoSuchElementException("Cita no encontrada: " + command.codigoCita()));

        cita.cancelar(Instant.now());
        citaRepositoryPort.save(cita);

        FranjaHoraria franja = franjaHorariaRepositoryPort.findById(cita.getFranjaHorariaId())
                .orElseThrow(() -> new NoSuchElementException("Franja: " + cita.getFranjaHorariaId()));
        franja.liberar();
        franjaHorariaRepositoryPort.save(franja);

        dispatchCancelacionNotificacion(cita, franja.getMedicoId());
    }

    private void dispatchCancelacionNotificacion(Cita cita, UUID medicoId) {
        Paciente paciente = pacienteRepositoryPort.findById(cita.getPacienteId())
                .orElse(null);
        Medico medico = medicoRepositoryPort.findById(medicoId).orElse(null);
        if (paciente == null || medico == null) return;

        UUID citaUuid = UUID.fromString(cita.getCodigo().getValue());
        Notificacion notificacion = Notificacion.nueva(citaUuid, TipoNotificacion.CANCELACION);
        Notificacion saved = notificacionRepositoryPort.save(notificacion);
        notificacionWhatsAppPort.enviar(saved, paciente, cita, medico);
    }
}
