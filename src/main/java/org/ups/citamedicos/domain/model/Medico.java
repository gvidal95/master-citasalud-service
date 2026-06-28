package org.ups.citamedicos.domain.model;

import java.util.UUID;

public class Medico {

    private final UUID id;
    private final String nombre;
    private final String especialidad;

    public Medico(UUID id, String nombre, String especialidad) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }
}
