package org.ups.citamedicos.domain.exception;

public class CitaDuplicadaException extends RuntimeException {

    public CitaDuplicadaException(String idempotencyKey) {
        super("Cita duplicada para idempotency-key: " + idempotencyKey);
    }
}
