package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.model.Notificacion;
import org.ups.citamedicos.domain.model.Paciente;

public interface NotificacionWhatsAppPort {

    void enviar(Notificacion notificacion, Paciente paciente, Cita cita, Medico medico);
}
