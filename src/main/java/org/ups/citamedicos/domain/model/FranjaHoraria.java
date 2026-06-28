package org.ups.citamedicos.domain.model;

import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class FranjaHoraria {

    private final UUID id;
    private final UUID medicoId;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private EstadoFranja estado;
    private Long version;

    public FranjaHoraria(UUID id, UUID medicoId, LocalDate fecha, LocalTime horaInicio,
                         LocalTime horaFin, EstadoFranja estado, Long version) {
        this.id = id;
        this.medicoId = medicoId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
        this.version = version;
    }

    public void reservar() {
        if (estado != EstadoFranja.LIBRE) {
            throw new FranjaNoDisponibleException(id, medicoId);
        }
        this.estado = EstadoFranja.OCUPADA;
    }

    public void liberar() {
        this.estado = EstadoFranja.LIBRE;
    }

    public UUID getId() { return id; }
    public UUID getMedicoId() { return medicoId; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public EstadoFranja getEstado() { return estado; }
    public Long getVersion() { return version; }
}
