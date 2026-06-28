package org.ups.citamedicos.adapter.in.web.mapper;

import org.ups.citamedicos.adapter.in.web.generated.dto.CitaResponse;
import org.ups.citamedicos.adapter.in.web.generated.dto.MedicoResponse;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class CitaMapper {

    private CitaMapper() {}

    public static CitaResponse toResponse(Cita cita, Medico medico, FranjaHoraria franja) {
        MedicoResponse medicoResponse = MedicoMapper.toResponse(medico);

        CitaResponse response = new CitaResponse();
        response.setCodigo(java.util.UUID.fromString(cita.getCodigo().getValue()));
        response.setPacienteId(cita.getPacienteId());
        response.setMedico(medicoResponse);
        response.setFecha(franja.getFecha());
        response.setHoraInicio(franja.getHoraInicio().toString());
        response.setHoraFin(franja.getHoraFin().toString());
        response.setEstado(CitaResponse.EstadoEnum.valueOf(cita.getEstado().name()));
        response.setCreadaEn(OffsetDateTime.ofInstant(cita.getFechaCreacion(), ZoneOffset.UTC));
        return response;
    }
}
