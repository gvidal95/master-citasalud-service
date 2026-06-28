package org.ups.citamedicos.adapter.out.persistence;

import org.ups.citamedicos.adapter.out.persistence.entity.CitaEntity;
import org.ups.citamedicos.adapter.out.persistence.entity.NotificacionEntity;
import org.ups.citamedicos.adapter.out.persistence.repository.CitaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.NotificacionJpaRepository;
import org.ups.citamedicos.application.port.out.NotificacionRepositoryPort;
import org.ups.citamedicos.domain.model.Notificacion;
import org.ups.citamedicos.domain.valueobject.CanalNotificacion;
import org.ups.citamedicos.domain.valueobject.EstadoEnvio;
import org.ups.citamedicos.domain.valueobject.TipoNotificacion;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class NotificacionJpaAdapter implements NotificacionRepositoryPort {

    private final NotificacionJpaRepository repository;
    private final CitaJpaRepository citaJpaRepository;

    public NotificacionJpaAdapter(NotificacionJpaRepository repository, CitaJpaRepository citaJpaRepository) {
        this.repository = repository;
        this.citaJpaRepository = citaJpaRepository;
    }

    @Override
    public Notificacion save(Notificacion notificacion) {
        CitaEntity citaEntity = citaJpaRepository.findById(notificacion.getCitaId().toString())
                .orElseThrow(() -> new NoSuchElementException("Cita: " + notificacion.getCitaId()));

        NotificacionEntity entity = NotificacionEntity.builder()
                .id(notificacion.getId().toString())
                .cita(citaEntity)
                .tipo(notificacion.getTipo().name())
                .canal(notificacion.getCanal().name())
                .estadoEnvio(notificacion.getEstadoEnvio().name())
                .creadaEn(notificacion.getFechaCreacion())
                .intentos(0)
                .build();

        NotificacionEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Notificacion> findPendientesConIntentosInsuficientes(int maxIntentos) {
        return repository.findPendientes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Notificacion toDomain(NotificacionEntity entity) {
        return new Notificacion(
                java.util.UUID.fromString(entity.getId()),
                java.util.UUID.fromString(entity.getCita().getId()),
                TipoNotificacion.valueOf(entity.getTipo()),
                CanalNotificacion.valueOf(entity.getCanal()),
                EstadoEnvio.valueOf(entity.getEstadoEnvio()),
                entity.getCreadaEn()
        );
    }
}
