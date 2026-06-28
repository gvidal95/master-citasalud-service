package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.Paciente;

import java.util.Optional;
import java.util.UUID;

public interface PacienteRepositoryPort {

    Optional<Paciente> findById(UUID id);
}
