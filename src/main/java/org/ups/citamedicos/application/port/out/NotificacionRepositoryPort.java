package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.Notificacion;

import java.util.List;

public interface NotificacionRepositoryPort {

    Notificacion save(Notificacion notificacion);

    List<Notificacion> findPendientesConIntentosInsuficientes(int maxIntentos);
}
