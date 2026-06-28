package org.ups.citamedicos.adapter.out.persistence;

import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;
import org.ups.citamedicos.application.port.out.PacienteRepositoryPort;
import org.ups.citamedicos.domain.model.Paciente;
import org.ups.citamedicos.domain.valueobject.NumeroWhatsApp;

import java.util.Optional;
import java.util.UUID;

public class PacienteJpaAdapter implements PacienteRepositoryPort {

    private final PacienteJpaRepository repository;

    public PacienteJpaAdapter(PacienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Paciente> findById(UUID id) {
        return repository.findById(id.toString()).map(entity ->
                new Paciente(
                        UUID.fromString(entity.getId()),
                        entity.getNombre(),
                        NumeroWhatsApp.of(entity.getTelefonoWhatsapp())
                )
        );
    }
}
