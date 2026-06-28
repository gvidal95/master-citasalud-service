package org.ups.citamedicos.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ups.citamedicos.adapter.out.persistence.entity.PacienteEntity;

import java.util.Optional;

public interface PacienteJpaRepository extends JpaRepository<PacienteEntity, String> {

    Optional<PacienteEntity> findByDocumento(String documento);
}
