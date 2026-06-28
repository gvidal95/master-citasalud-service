package org.ups.citamedicos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private CitaEntity cita;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(name = "estado_envio", nullable = false, length = 20)
    private String estadoEnvio;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "ultimo_intento_en")
    private Instant ultimoIntentoEn;
}
