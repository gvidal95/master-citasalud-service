package org.ups.citamedicos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "franjas_horarias",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medico_id", "fecha", "hora_inicio"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranjaHorariaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "medico_id", nullable = false, length = 36)
    private String medicoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 20)
    private String estado;

    @Version
    @Column(nullable = false)
    private Long version;
}
