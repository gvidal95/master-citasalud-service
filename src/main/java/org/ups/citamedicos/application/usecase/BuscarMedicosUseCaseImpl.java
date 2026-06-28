package org.ups.citamedicos.application.usecase;

import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.Medico;

import java.util.List;

public class BuscarMedicosUseCaseImpl implements BuscarMedicosUseCase {

    private final MedicoRepositoryPort medicoRepositoryPort;

    public BuscarMedicosUseCaseImpl(MedicoRepositoryPort medicoRepositoryPort) {
        this.medicoRepositoryPort = medicoRepositoryPort;
    }

    @Override
    public List<Medico> execute(BuscarMedicosQuery query) {
        return medicoRepositoryPort.findByEspecialidadAndNombre(query.especialidad(), query.nombre());
    }
}
