package org.ups.citamedicos.adapter.in.web.mapper;

import org.ups.citamedicos.adapter.in.web.generated.dto.MedicoResponse;
import org.ups.citamedicos.domain.model.Medico;

public final class MedicoMapper {

    private MedicoMapper() {}

    public static MedicoResponse toResponse(Medico medico) {
        MedicoResponse response = new MedicoResponse();
        response.setId(medico.getId());
        response.setNombre(medico.getNombre());
        response.setEspecialidad(medico.getEspecialidad());
        return response;
    }
}
