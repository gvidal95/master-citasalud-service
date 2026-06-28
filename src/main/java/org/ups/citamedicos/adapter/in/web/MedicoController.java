package org.ups.citamedicos.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ups.citamedicos.adapter.in.web.generated.MedicosApi;
import org.ups.citamedicos.adapter.in.web.generated.dto.FranjaHorariaResponse;
import org.ups.citamedicos.adapter.in.web.generated.dto.MedicoResponse;
import org.ups.citamedicos.adapter.in.web.mapper.FranjaHorariaMapper;
import org.ups.citamedicos.adapter.in.web.mapper.MedicoMapper;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase.BuscarMedicosQuery;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase.ConsultarDisponibilidadQuery;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MedicoController implements MedicosApi {

    private final BuscarMedicosUseCase buscarMedicosUseCase;
    private final ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    public MedicoController(BuscarMedicosUseCase buscarMedicosUseCase,
                            ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase) {
        this.buscarMedicosUseCase = buscarMedicosUseCase;
        this.consultarDisponibilidadUseCase = consultarDisponibilidadUseCase;
    }

    @Override
    public ResponseEntity<List<MedicoResponse>> buscarMedicos(String especialidad, String nombre) {
        List<MedicoResponse> medicos = buscarMedicosUseCase.execute(new BuscarMedicosQuery(especialidad, nombre))
                .stream()
                .map(MedicoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }

    @Override
    public ResponseEntity<List<FranjaHorariaResponse>> consultarDisponibilidad(
            UUID medicoId, LocalDate fechaDesde, LocalDate fechaHasta) {
        ConsultarDisponibilidadQuery query = new ConsultarDisponibilidadQuery(
                medicoId, fechaDesde, fechaHasta, EstadoFranja.LIBRE);
        List<FranjaHorariaResponse> franjas = consultarDisponibilidadUseCase.execute(query)
                .stream()
                .map(FranjaHorariaMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(franjas);
    }
}
