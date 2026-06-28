package org.ups.citamedicos.adapter.out.persistence;

import jakarta.persistence.OptimisticLockException;
import org.ups.citamedicos.adapter.out.persistence.entity.FranjaHorariaEntity;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FranjaHorariaJpaAdapter implements FranjaHorariaRepositoryPort {

    private final FranjaHorariaJpaRepository repository;

    public FranjaHorariaJpaAdapter(FranjaHorariaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<FranjaHoraria> findById(UUID id) {
        return repository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public FranjaHoraria save(FranjaHoraria franja) {
        try {
            FranjaHorariaEntity entity = toEntity(franja);
            return toDomain(repository.save(entity));
        } catch (OptimisticLockException e) {
            throw new FranjaNoDisponibleException(franja.getId(), franja.getMedicoId());
        }
    }

    @Override
    public List<FranjaHoraria> findByMedicoAndRangoFecha(UUID medicoId, LocalDate desde, LocalDate hasta, EstadoFranja estadoFiltro) {
        String estadoStr = estadoFiltro != null ? estadoFiltro.name() : null;
        return repository.findByMedicoIdAndFechaBetweenAndEstado(medicoId.toString(), desde, hasta, estadoStr)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private FranjaHoraria toDomain(FranjaHorariaEntity entity) {
        return new FranjaHoraria(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getMedicoId()),
                entity.getFecha(),
                entity.getHoraInicio(),
                entity.getHoraFin(),
                EstadoFranja.valueOf(entity.getEstado()),
                entity.getVersion()
        );
    }

    private FranjaHorariaEntity toEntity(FranjaHoraria franja) {
        return FranjaHorariaEntity.builder()
                .id(franja.getId().toString())
                .medicoId(franja.getMedicoId().toString())
                .fecha(franja.getFecha())
                .horaInicio(franja.getHoraInicio())
                .horaFin(franja.getHoraFin())
                .estado(franja.getEstado().name())
                .version(franja.getVersion())
                .build();
    }
}
