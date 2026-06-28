package org.ups.citamedicos.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.ups.citamedicos.adapter.out.persistence.entity.FranjaHorariaEntity;

import java.time.LocalDate;
import java.util.List;

public interface FranjaHorariaJpaRepository extends JpaRepository<FranjaHorariaEntity, String> {

    @Query("""
            SELECT f FROM FranjaHorariaEntity f
            WHERE f.medicoId = :medicoId
            AND f.fecha BETWEEN :desde AND :hasta
            AND (:estado IS NULL OR f.estado = :estado)
            """)
    List<FranjaHorariaEntity> findByMedicoIdAndFechaBetweenAndEstado(
            @Param("medicoId") String medicoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estado") String estado);
}
