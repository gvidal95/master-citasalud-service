package org.ups.citamedicos.adapter.in.web.mapper;

import org.ups.citamedicos.adapter.in.web.generated.dto.FranjaHorariaResponse;
import org.ups.citamedicos.domain.model.FranjaHoraria;

public final class FranjaHorariaMapper {

    private FranjaHorariaMapper() {}

    public static FranjaHorariaResponse toResponse(FranjaHoraria franja) {
        FranjaHorariaResponse response = new FranjaHorariaResponse();
        response.setId(franja.getId());
        response.setMedicoId(franja.getMedicoId());
        response.setFecha(franja.getFecha());
        response.setHoraInicio(franja.getHoraInicio().toString());
        response.setHoraFin(franja.getHoraFin().toString());
        response.setEstado(FranjaHorariaResponse.EstadoEnum.valueOf(franja.getEstado().name()));
        return response;
    }
}
