package org.ups.citamedicos.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.ups.citamedicos.adapter.in.web.generated.dto.ErrorResponse;
import org.ups.citamedicos.adapter.in.web.generated.dto.FranjaHorariaResponse;
import org.ups.citamedicos.adapter.in.web.generated.dto.FranjaNoDisponibleErrorResponse;
import org.ups.citamedicos.adapter.in.web.mapper.FranjaHorariaMapper;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase.ConsultarDisponibilidadQuery;
import org.ups.citamedicos.domain.exception.CancelacionFueraDePlazoException;
import org.ups.citamedicos.domain.exception.CitaDuplicadaException;
import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    public GlobalExceptionHandler(ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase) {
        this.consultarDisponibilidadUseCase = consultarDisponibilidadUseCase;
    }

    @ExceptionHandler(FranjaNoDisponibleException.class)
    public ResponseEntity<FranjaNoDisponibleErrorResponse> handleFranjaNoDisponible(FranjaNoDisponibleException ex) {
        List<FranjaHorariaResponse> alternativas = fetchAlternativas(ex);

        FranjaNoDisponibleErrorResponse response = new FranjaNoDisponibleErrorResponse();
        response.setCodigo("FRANJA_NO_DISPONIBLE");
        response.setMensaje("La franja horaria seleccionada ya no está disponible.");
        response.setDetalle("Por favor elija otra franja horaria para el médico indicado.");
        response.setAlternativas(alternativas);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(CitaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleCitaDuplicada(CitaDuplicadaException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("CITA_DUPLICADA");
        response.setMensaje(ex.getMessage());
        response.setDetalle("Ya existe una cita confirmada para este paciente en la misma fecha y médico.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(CancelacionFueraDePlazoException.class)
    public ResponseEntity<ErrorResponse> handleCancelacionFueraDePlazo(CancelacionFueraDePlazoException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("CANCELACION_FUERA_DE_PLAZO");
        response.setMensaje(ex.getMessage());
        response.setDetalle("Solo se pueden cancelar citas con al menos 24 horas de antelación.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("ESTADO_INVALIDO");
        response.setMensaje(ex.getMessage());
        response.setDetalle("La operación no es válida para el estado actual del recurso.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("PARAMETRO_INVALIDO");
        response.setMensaje(ex.getMessage());
        response.setDetalle("Verifique los parámetros de la solicitud.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("RECURSO_NO_ENCONTRADO");
        response.setMensaje(ex.getMessage());
        response.setDetalle("El recurso solicitado no existe.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo("ERROR_INTERNO");
        response.setMensaje("Ha ocurrido un error interno.");
        response.setDetalle(ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private List<FranjaHorariaResponse> fetchAlternativas(FranjaNoDisponibleException ex) {
        try {
            ConsultarDisponibilidadQuery query = new ConsultarDisponibilidadQuery(
                    ex.getMedicoId(),
                    LocalDate.now(),
                    LocalDate.now().plusDays(7),
                    EstadoFranja.LIBRE
            );
            return consultarDisponibilidadUseCase.execute(query)
                    .stream()
                    .limit(5)
                    .map(FranjaHorariaMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
