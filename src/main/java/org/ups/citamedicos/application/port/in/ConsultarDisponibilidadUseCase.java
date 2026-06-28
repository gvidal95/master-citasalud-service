package org.ups.citamedicos.application.port.in;

import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsultarDisponibilidadUseCase {

    record ConsultarDisponibilidadQuery(UUID medicoId, LocalDate fechaDesde, LocalDate hasta, EstadoFranja estadoFiltro) {}

    List<FranjaHoraria> execute(ConsultarDisponibilidadQuery query);
}
