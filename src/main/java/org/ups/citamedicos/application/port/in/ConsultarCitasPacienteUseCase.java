package org.ups.citamedicos.application.port.in;

import org.ups.citamedicos.domain.model.Cita;

import java.util.List;
import java.util.UUID;

public interface ConsultarCitasPacienteUseCase {

    record ConsultarCitasPacienteQuery(UUID pacienteId) {}

    List<Cita> execute(ConsultarCitasPacienteQuery query);
}
