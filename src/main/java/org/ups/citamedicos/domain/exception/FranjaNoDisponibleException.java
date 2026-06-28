package org.ups.citamedicos.domain.exception;

import java.util.UUID;

public class FranjaNoDisponibleException extends RuntimeException {

    private final UUID franjaId;
    private final UUID medicoId;

    public FranjaNoDisponibleException(UUID franjaId, UUID medicoId) {
        super("Franja horaria no disponible: " + franjaId);
        this.franjaId = franjaId;
        this.medicoId = medicoId;
    }

    public UUID getFranjaId() {
        return franjaId;
    }

    public UUID getMedicoId() {
        return medicoId;
    }
}
