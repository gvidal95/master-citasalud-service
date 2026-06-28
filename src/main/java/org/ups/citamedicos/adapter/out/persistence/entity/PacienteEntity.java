package org.ups.citamedicos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pacientes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "telefono_whatsapp", nullable = false, length = 20)
    private String telefonoWhatsapp;

    @Column(length = 100)
    private String email;
}
