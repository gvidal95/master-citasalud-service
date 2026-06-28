package org.ups.citamedicos.application.usecase;

import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.out.CitaRepositoryPort;
import org.ups.citamedicos.application.port.out.PacienteRepositoryPort;
import org.ups.citamedicos.domain.model.Cita;

import java.util.List;
import java.util.NoSuchElementException;

public class ConsultarCitasPacienteUseCaseImpl implements ConsultarCitasPacienteUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    public ConsultarCitasPacienteUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                             PacienteRepositoryPort pacienteRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.pacienteRepositoryPort = pacienteRepositoryPort;
    }

    @Override
    public List<Cita> execute(ConsultarCitasPacienteQuery query) {
        pacienteRepositoryPort.findById(query.pacienteId())
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado: " + query.pacienteId()));
        return citaRepositoryPort.findConfirmadasByPacienteId(query.pacienteId());
    }
}
