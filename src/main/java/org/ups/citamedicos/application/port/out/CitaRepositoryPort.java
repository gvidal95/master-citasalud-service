package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.valueobject.CodigoCita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CitaRepositoryPort {

    Cita save(Cita cita);

    Optional<Cita> findByCodigo(CodigoCita codigo);

    Optional<Cita> findByIdempotencyKey(String idempotencyKey);

    boolean existsConfirmadaByPacienteAndMedicoAndFecha(UUID pacienteId, UUID medicoId, LocalDate fecha);

    Optional<Cita> findByCodigoForUpdate(CodigoCita codigo);

    List<Cita> findConfirmadasByPacienteId(UUID pacienteId);
}
