package org.ups.citamedicos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String especialidad;

    @Column(name = "lugar_atencion", length = 200)
    private String lugarAtencion;
}
