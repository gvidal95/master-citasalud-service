package org.ups.citamedicos.adapter.out.persistence;

import org.ups.citamedicos.adapter.out.persistence.repository.MedicoJpaRepository;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.Medico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MedicoJpaAdapter implements MedicoRepositoryPort {

    private final MedicoJpaRepository repository;

    public MedicoJpaAdapter(MedicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Medico> findByEspecialidadAndNombre(String especialidad, String nombre) {
        boolean hasEsp = especialidad != null && !especialidad.isBlank();
        boolean hasNom = nombre != null && !nombre.isBlank();
        if (!hasEsp && !hasNom) {
            return repository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (hasEsp && !hasNom) {
            return repository.findByEspecialidadContainingIgnoreCase(especialidad)
                    .stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (!hasEsp) {
            return repository.findByNombreContainingIgnoreCase(nombre)
                    .stream().map(this::toDomain).collect(Collectors.toList());
        }
        return repository.findByEspecialidadContainingIgnoreCaseOrNombreContainingIgnoreCase(especialidad, nombre)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Medico> findById(UUID id) {
        return repository.findById(id.toString()).map(this::toDomain);
    }

    private Medico toDomain(org.ups.citamedicos.adapter.out.persistence.entity.MedicoEntity entity) {
        return new Medico(UUID.fromString(entity.getId()), entity.getNombre(), entity.getEspecialidad());
    }
}
