package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.Medico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicoRepositoryPort {

    List<Medico> findByEspecialidadAndNombre(String especialidad, String nombre);

    Optional<Medico> findById(UUID id);
}
