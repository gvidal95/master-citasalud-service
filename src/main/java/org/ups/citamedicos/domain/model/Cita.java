package org.ups.citamedicos.domain.model;

import org.ups.citamedicos.domain.exception.CancelacionFueraDePlazoException;
import org.ups.citamedicos.domain.valueobject.CodigoCita;
import org.ups.citamedicos.domain.valueobject.EstadoCita;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Cita {

    private final CodigoCita codigo;
    private final UUID pacienteId;
    private final UUID franjaHorariaId;
    private final String idempotencyKey;
    private final Instant fechaCreacion;
    private final LocalDateTime franjaFechaHoraInicio;
    private EstadoCita estado;

    private Cita(CodigoCita codigo, UUID pacienteId, UUID franjaHorariaId,
                 String idempotencyKey, Instant fechaCreacion,
                 LocalDateTime franjaFechaHoraInicio, EstadoCita estado) {
        this.codigo = codigo;
        this.pacienteId = pacienteId;
        this.franjaHorariaId = franjaHorariaId;
        this.idempotencyKey = idempotencyKey;
        this.fechaCreacion = fechaCreacion;
        this.franjaFechaHoraInicio = franjaFechaHoraInicio;
        this.estado = estado;
    }

    public static Cita nueva(UUID pacienteId, UUID franjaHorariaId,
                             String idempotencyKey, LocalDateTime franjaFechaHoraInicio) {
        return new Cita(
                CodigoCita.generate(),
                pacienteId,
                franjaHorariaId,
                idempotencyKey,
                Instant.now(),
                franjaFechaHoraInicio,
                EstadoCita.CONFIRMADA
        );
    }

    public static Cita reconstituir(CodigoCita codigo, UUID pacienteId, UUID franjaHorariaId,
                                    String idempotencyKey, Instant fechaCreacion,
                                    LocalDateTime franjaFechaHoraInicio, EstadoCita estado) {
        return new Cita(codigo, pacienteId, franjaHorariaId, idempotencyKey,
                fechaCreacion, franjaFechaHoraInicio, estado);
    }

    public void cancelar(Instant ahora) {
        if (estado == EstadoCita.CANCELADA) {
            throw new IllegalStateException("La cita " + codigo.getValue() + " ya está cancelada");
        }
        Instant franjaInstant = franjaFechaHoraInicio.toInstant(ZoneOffset.UTC);
        long horasHastaCita = ChronoUnit.HOURS.between(ahora, franjaInstant);
        if (horasHastaCita < 24) {
            throw new CancelacionFueraDePlazoException(codigo);
        }
        this.estado = EstadoCita.CANCELADA;
    }

    public CodigoCita getCodigo() { return codigo; }
    public UUID getPacienteId() { return pacienteId; }
    public UUID getFranjaHorariaId() { return franjaHorariaId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFranjaFechaHoraInicio() { return franjaFechaHoraInicio; }
    public EstadoCita getEstado() { return estado; }
}
