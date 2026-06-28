package org.ups.citamedicos.application.port.out;

import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FranjaHorariaRepositoryPort {

    Optional<FranjaHoraria> findById(UUID id);

    FranjaHoraria save(FranjaHoraria franjaHoraria);

    List<FranjaHoraria> findByMedicoAndRangoFecha(UUID medicoId, LocalDate desde, LocalDate hasta, EstadoFranja estadoFiltro);
}
