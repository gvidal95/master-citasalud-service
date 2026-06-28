package org.ups.citamedicos.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.ups.citamedicos.adapter.out.persistence.entity.NotificacionEntity;

import java.util.List;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, String> {

    @Query("""
            SELECT n FROM NotificacionEntity n
            WHERE n.estadoEnvio = 'PENDIENTE'
            """)
    List<NotificacionEntity> findPendientes();

    @Query("SELECT n FROM NotificacionEntity n WHERE n.estadoEnvio IN ('PENDIENTE', 'FALLIDA')")
    List<NotificacionEntity> findPendientesOFallidas(@Param("maxIntentos") int maxIntentos);
}
