package org.ups.citamedicos.adapter.out.persistence;

import org.ups.citamedicos.adapter.out.persistence.entity.CitaEntity;
import org.ups.citamedicos.adapter.out.persistence.entity.FranjaHorariaEntity;
import org.ups.citamedicos.adapter.out.persistence.entity.MedicoEntity;
import org.ups.citamedicos.adapter.out.persistence.entity.PacienteEntity;
import org.ups.citamedicos.adapter.out.persistence.repository.CitaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.FranjaHorariaJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.MedicoJpaRepository;
import org.ups.citamedicos.adapter.out.persistence.repository.PacienteJpaRepository;
import org.ups.citamedicos.application.port.out.CitaRepositoryPort;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.valueobject.CodigoCita;
import org.ups.citamedicos.domain.valueobject.EstadoCita;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CitaJpaAdapter implements CitaRepositoryPort {

    private final CitaJpaRepository citaJpaRepository;
    private final PacienteJpaRepository pacienteJpaRepository;
    private final MedicoJpaRepository medicoJpaRepository;
    private final FranjaHorariaJpaRepository franjaHorariaJpaRepository;

    public CitaJpaAdapter(CitaJpaRepository citaJpaRepository,
                          PacienteJpaRepository pacienteJpaRepository,
                          MedicoJpaRepository medicoJpaRepository,
                          FranjaHorariaJpaRepository franjaHorariaJpaRepository) {
        this.citaJpaRepository = citaJpaRepository;
        this.pacienteJpaRepository = pacienteJpaRepository;
        this.medicoJpaRepository = medicoJpaRepository;
        this.franjaHorariaJpaRepository = franjaHorariaJpaRepository;
    }

    @Override
    public Cita save(Cita cita) {
        PacienteEntity paciente = pacienteJpaRepository.findById(cita.getPacienteId().toString())
                .orElseThrow(() -> new NoSuchElementException("Paciente: " + cita.getPacienteId()));
        FranjaHorariaEntity franja = franjaHorariaJpaRepository.findById(cita.getFranjaHorariaId().toString())
                .orElseThrow(() -> new NoSuchElementException("Franja: " + cita.getFranjaHorariaId()));
        MedicoEntity medico = medicoJpaRepository.findById(franja.getMedicoId())
                .orElseThrow(() -> new NoSuchElementException("Médico: " + franja.getMedicoId()));

        CitaEntity entity = CitaEntity.builder()
                .id(cita.getCodigo().getValue())
                .codigo(cita.getCodigo().getValue())
                .paciente(paciente)
                .medico(medico)
                .franjaHoraria(franja)
                .idempotencyKey(cita.getIdempotencyKey())
                .estado(cita.getEstado().name())
                .fechaCreacion(cita.getFechaCreacion())
                .franjaFechaHoraInicio(cita.getFranjaFechaHoraInicio())
                .build();

        CitaEntity saved = citaJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Cita> findByCodigo(CodigoCita codigo) {
        return citaJpaRepository.findByCodigo(codigo.getValue()).map(this::toDomain);
    }

    @Override
    public Optional<Cita> findByIdempotencyKey(String idempotencyKey) {
        return citaJpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public boolean existsConfirmadaByPacienteAndMedicoAndFecha(UUID pacienteId, UUID medicoId, LocalDate fecha) {
        return citaJpaRepository.existsConfirmadaByPacienteAndMedicoAndFecha(
                pacienteId.toString(), medicoId.toString(), fecha);
    }

    @Override
    public Optional<Cita> findByCodigoForUpdate(CodigoCita codigo) {
        return citaJpaRepository.findByCodigo(codigo.getValue()).map(this::toDomain);
    }

    @Override
    public List<Cita> findConfirmadasByPacienteId(UUID pacienteId) {
        return citaJpaRepository.findByPacienteIdAndEstadoOrderByFranjaFechaHoraInicioAsc(
                        pacienteId.toString(), EstadoCita.CONFIRMADA.name())
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Cita toDomain(CitaEntity entity) {
        return Cita.reconstituir(
                CodigoCita.of(entity.getCodigo()),
                UUID.fromString(entity.getPaciente().getId()),
                UUID.fromString(entity.getFranjaHoraria().getId()),
                entity.getIdempotencyKey(),
                entity.getFechaCreacion(),
                entity.getFranjaFechaHoraInicio(),
                EstadoCita.valueOf(entity.getEstado())
        );
    }
}
