package org.ups.citamedicos.application.port.in;

import org.ups.citamedicos.domain.model.Cita;

import java.util.UUID;

public interface ReservarCitaUseCase {

    record ReservarCitaCommand(UUID pacienteId, UUID franjaHorariaId, String idempotencyKey) {}

    Cita execute(ReservarCitaCommand command);
}
