package org.ups.citamedicos.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.ups.citamedicos.adapter.out.persistence.entity.CitaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CitaJpaRepository extends JpaRepository<CitaEntity, String> {

    Optional<CitaEntity> findByCodigo(String codigo);

    Optional<CitaEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM CitaEntity c
            WHERE c.paciente.id = :pacienteId
            AND c.medico.id = :medicoId
            AND CAST(c.franjaFechaHoraInicio AS localdate) = :fecha
            AND c.estado = 'CONFIRMADA'
            """)
    boolean existsConfirmadaByPacienteAndMedicoAndFecha(
            @Param("pacienteId") String pacienteId,
            @Param("medicoId") String medicoId,
            @Param("fecha") LocalDate fecha);

    List<CitaEntity> findByPacienteIdAndEstadoOrderByFranjaFechaHoraInicioAsc(String pacienteId, String estado);
}
