package org.ups.citamedicos.application.port.in;

import org.ups.citamedicos.domain.model.Medico;

import java.util.List;

public interface BuscarMedicosUseCase {

    record BuscarMedicosQuery(String especialidad, String nombre) {}

    List<Medico> execute(BuscarMedicosQuery query);
}
