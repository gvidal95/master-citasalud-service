package org.ups.citamedicos.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ups.citamedicos.adapter.in.web.generated.CitasApi;
import org.ups.citamedicos.adapter.in.web.generated.dto.CitaResponse;
import org.ups.citamedicos.adapter.in.web.generated.dto.ReservarCitaRequest;
import org.ups.citamedicos.adapter.in.web.mapper.CitaMapper;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase.CancelarCitaCommand;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase.ConsultarCitasPacienteQuery;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase.ReservarCitaCommand;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.valueobject.CodigoCita;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class CitaController implements CitasApi {

    private final ReservarCitaUseCase reservarCitaUseCase;
    private final CancelarCitaUseCase cancelarCitaUseCase;
    private final ConsultarCitasPacienteUseCase consultarCitasPacienteUseCase;
    private final MedicoRepositoryPort medicoRepositoryPort;
    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;

    public CitaController(ReservarCitaUseCase reservarCitaUseCase,
                          CancelarCitaUseCase cancelarCitaUseCase,
                          ConsultarCitasPacienteUseCase consultarCitasPacienteUseCase,
                          MedicoRepositoryPort medicoRepositoryPort,
                          FranjaHorariaRepositoryPort franjaHorariaRepositoryPort) {
        this.reservarCitaUseCase = reservarCitaUseCase;
        this.cancelarCitaUseCase = cancelarCitaUseCase;
        this.consultarCitasPacienteUseCase = consultarCitasPacienteUseCase;
        this.medicoRepositoryPort = medicoRepositoryPort;
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
    }

    @Override
    public ResponseEntity<CitaResponse> reservarCita(ReservarCitaRequest request, UUID idempotencyKey) {
        String idemKey = idempotencyKey != null ? idempotencyKey.toString() : UUID.randomUUID().toString();
        ReservarCitaCommand command = new ReservarCitaCommand(
                request.getPacienteId(),
                request.getFranjaHorariaId(),
                idemKey
        );
        Cita cita = reservarCitaUseCase.execute(command);
        CitaResponse response = buildCitaResponse(cita);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> cancelarCita(UUID codigoCita) {
        cancelarCitaUseCase.execute(new CancelarCitaCommand(CodigoCita.of(codigoCita.toString())));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CitaResponse>> consultarCitasPaciente(UUID pacienteId) {
        List<Cita> citas = consultarCitasPacienteUseCase.execute(new ConsultarCitasPacienteQuery(pacienteId));
        List<CitaResponse> responses = citas.stream()
                .map(this::buildCitaResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private CitaResponse buildCitaResponse(Cita cita) {
        FranjaHoraria franja = franjaHorariaRepositoryPort.findById(cita.getFranjaHorariaId())
                .orElseThrow(() -> new NoSuchElementException("Franja: " + cita.getFranjaHorariaId()));
        Medico medico = medicoRepositoryPort.findById(franja.getMedicoId())
                .orElseThrow(() -> new NoSuchElementException("Médico: " + franja.getMedicoId()));
        return CitaMapper.toResponse(cita, medico, franja);
    }
}
