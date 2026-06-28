package org.ups.citamedicos.domain.exception;

import org.ups.citamedicos.domain.valueobject.CodigoCita;

public class CancelacionFueraDePlazoException extends RuntimeException {

    public CancelacionFueraDePlazoException(CodigoCita codigoCita) {
        super("No se puede cancelar la cita " + codigoCita.getValue() + ": menos de 24 horas para la cita");
    }
}
