package org.ups.citamedicos.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ups.citamedicos.adapter.out.persistence.entity.MedicoEntity;

import java.util.List;

public interface MedicoJpaRepository extends JpaRepository<MedicoEntity, String> {

    List<MedicoEntity> findByEspecialidadContainingIgnoreCase(String especialidad);

    List<MedicoEntity> findByNombreContainingIgnoreCase(String nombre);

    List<MedicoEntity> findByEspecialidadContainingIgnoreCaseOrNombreContainingIgnoreCase(
            String especialidad, String nombre);
}
