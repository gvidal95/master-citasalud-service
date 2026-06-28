package org.ups.citamedicos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 36)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacienteEntity paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private MedicoEntity medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_horaria_id", nullable = false)
    private FranjaHorariaEntity franjaHoraria;

    @Column(name = "idempotency_key", unique = true, length = 36)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "franja_fecha_hora_inicio", nullable = false)
    private LocalDateTime franjaFechaHoraInicio;
}
