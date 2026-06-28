package org.ups.citamedicos.domain.model;

import org.ups.citamedicos.domain.valueobject.CanalNotificacion;
import org.ups.citamedicos.domain.valueobject.EstadoEnvio;
import org.ups.citamedicos.domain.valueobject.TipoNotificacion;

import java.time.Instant;
import java.util.UUID;

public class Notificacion {

    private final UUID id;
    private final UUID citaId;
    private final TipoNotificacion tipo;
    private final CanalNotificacion canal;
    private EstadoEnvio estadoEnvio;
    private final Instant fechaCreacion;

    public Notificacion(UUID id, UUID citaId, TipoNotificacion tipo,
                        CanalNotificacion canal, EstadoEnvio estadoEnvio, Instant fechaCreacion) {
        this.id = id;
        this.citaId = citaId;
        this.tipo = tipo;
        this.canal = canal;
        this.estadoEnvio = estadoEnvio;
        this.fechaCreacion = fechaCreacion;
    }

    public static Notificacion nueva(UUID citaId, TipoNotificacion tipo) {
        return new Notificacion(
                UUID.randomUUID(),
                citaId,
                tipo,
                CanalNotificacion.WHATSAPP,
                EstadoEnvio.PENDIENTE,
                Instant.now()
        );
    }

    public void marcarEnviada() {
        this.estadoEnvio = EstadoEnvio.ENVIADA;
    }

    public void marcarFallida() {
        this.estadoEnvio = EstadoEnvio.FALLIDA;
    }

    public UUID getId() { return id; }
    public UUID getCitaId() { return citaId; }
    public TipoNotificacion getTipo() { return tipo; }
    public CanalNotificacion getCanal() { return canal; }
    public EstadoEnvio getEstadoEnvio() { return estadoEnvio; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
