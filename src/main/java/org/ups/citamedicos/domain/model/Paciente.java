package org.ups.citamedicos.domain.model;

import org.ups.citamedicos.domain.valueobject.NumeroWhatsApp;

import java.util.UUID;

public class Paciente {

    private final UUID id;
    private final String nombre;
    private final NumeroWhatsApp numeroWhatsApp;

    public Paciente(UUID id, String nombre, NumeroWhatsApp numeroWhatsApp) {
        this.id = id;
        this.nombre = nombre;
        this.numeroWhatsApp = numeroWhatsApp;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public NumeroWhatsApp getNumeroWhatsApp() {
        return numeroWhatsApp;
    }
}
