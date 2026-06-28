package org.ups.citamedicos.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ups.citamedicos.adapter.out.notification.WhatsAppCloudApiAdapter;
import org.ups.citamedicos.adapter.out.persistence.*;
import org.ups.citamedicos.adapter.out.persistence.repository.*;
import org.ups.citamedicos.application.port.out.*;
import org.ups.citamedicos.application.usecase.*;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class UseCaseConfig {

    @Bean
    public PacienteRepositoryPort pacienteRepositoryPort(PacienteJpaRepository repo) {
        return new PacienteJpaAdapter(repo);
    }

    @Bean
    public MedicoRepositoryPort medicoRepositoryPort(MedicoJpaRepository repo) {
        return new MedicoJpaAdapter(repo);
    }

    @Bean
    public FranjaHorariaRepositoryPort franjaHorariaRepositoryPort(FranjaHorariaJpaRepository repo) {
        return new FranjaHorariaJpaAdapter(repo);
    }

    @Bean
    public CitaRepositoryPort citaRepositoryPort(CitaJpaRepository citaRepo,
                                                  PacienteJpaRepository pacienteRepo,
                                                  MedicoJpaRepository medicoRepo,
                                                  FranjaHorariaJpaRepository franjaRepo) {
        return new CitaJpaAdapter(citaRepo, pacienteRepo, medicoRepo, franjaRepo);
    }

    @Bean
    public NotificacionRepositoryPort notificacionRepositoryPort(NotificacionJpaRepository repo,
                                                                  CitaJpaRepository citaRepo) {
        return new NotificacionJpaAdapter(repo, citaRepo);
    }

    @Bean
    public NotificacionWhatsAppPort notificacionWhatsAppPort(WhatsAppProperties properties) {
        return new WhatsAppCloudApiAdapter(properties);
    }

    @Bean
    public ReservarCitaUseCaseImpl reservarCitaUseCase(CitaRepositoryPort citaPort,
                                                        FranjaHorariaRepositoryPort franjaPort,
                                                        PacienteRepositoryPort pacientePort,
                                                        MedicoRepositoryPort medicoPort,
                                                        NotificacionWhatsAppPort whatsAppPort,
                                                        NotificacionRepositoryPort notificacionPort) {
        return new ReservarCitaUseCaseImpl(citaPort, franjaPort, pacientePort, medicoPort, whatsAppPort, notificacionPort);
    }

    @Bean
    public BuscarMedicosUseCaseImpl buscarMedicosUseCase(MedicoRepositoryPort medicoPort) {
        return new BuscarMedicosUseCaseImpl(medicoPort);
    }

    @Bean
    public ConsultarDisponibilidadUseCaseImpl consultarDisponibilidadUseCase(
            FranjaHorariaRepositoryPort franjaPort,
            MedicoRepositoryPort medicoPort) {
        return new ConsultarDisponibilidadUseCaseImpl(franjaPort, medicoPort);
    }

    @Bean
    public CancelarCitaUseCaseImpl cancelarCitaUseCase(CitaRepositoryPort citaPort,
                                                        FranjaHorariaRepositoryPort franjaPort,
                                                        NotificacionWhatsAppPort whatsAppPort,
                                                        NotificacionRepositoryPort notificacionPort,
                                                        PacienteRepositoryPort pacientePort,
                                                        MedicoRepositoryPort medicoPort) {
        return new CancelarCitaUseCaseImpl(citaPort, franjaPort, whatsAppPort, notificacionPort, pacientePort, medicoPort);
    }

    @Bean
    public ConsultarCitasPacienteUseCaseImpl consultarCitasPacienteUseCase(
            CitaRepositoryPort citaPort,
            PacienteRepositoryPort pacientePort) {
        return new ConsultarCitasPacienteUseCaseImpl(citaPort, pacientePort);
    }
}
