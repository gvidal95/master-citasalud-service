package org.ups.citamedicos.application.usecase;

import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.FranjaHoraria;

import java.util.List;
import java.util.NoSuchElementException;

public class ConsultarDisponibilidadUseCaseImpl implements ConsultarDisponibilidadUseCase {

    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;

    public ConsultarDisponibilidadUseCaseImpl(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                              MedicoRepositoryPort medicoRepositoryPort) {
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.medicoRepositoryPort = medicoRepositoryPort;
    }

    @Override
    public List<FranjaHoraria> execute(ConsultarDisponibilidadQuery query) {
        long days = query.hasta().toEpochDay() - query.fechaDesde().toEpochDay();
        if (days > 60) {
            throw new IllegalArgumentException("El rango de fechas no puede superar 60 días");
        }
        medicoRepositoryPort.findById(query.medicoId())
                .orElseThrow(() -> new NoSuchElementException("Médico no encontrado: " + query.medicoId()));

        return franjaHorariaRepositoryPort.findByMedicoAndRangoFecha(
                query.medicoId(), query.fechaDesde(), query.hasta(), query.estadoFiltro());
    }
}
