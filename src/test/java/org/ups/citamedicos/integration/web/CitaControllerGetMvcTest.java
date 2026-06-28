package org.ups.citamedicos.integration.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ups.citamedicos.adapter.in.web.CitaController;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
class CitaControllerGetMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservarCitaUseCase reservarCitaUseCase;

    @MockitoBean
    private CancelarCitaUseCase cancelarCitaUseCase;

    @MockitoBean
    private ConsultarCitasPacienteUseCase consultarCitasPacienteUseCase;

    @MockitoBean
    private MedicoRepositoryPort medicoRepositoryPort;

    @MockitoBean
    private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;

    @MockitoBean
    private ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    @Test
    void given_pacienteWithCitas_when_getCitas_then_200_withList() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        UUID franjaId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();

        Cita cita = Cita.nueva(pacienteId, franjaId, UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(5));
        FranjaHoraria franja = new FranjaHoraria(franjaId, medicoId, LocalDate.now().plusDays(5),
                LocalTime.of(10, 0), LocalTime.of(10, 30), EstadoFranja.OCUPADA, 1L);
        Medico medico = new Medico(medicoId, "Dr. García", "Cardiología");

        when(consultarCitasPacienteUseCase.execute(any())).thenReturn(List.of(cita));
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franja));
        when(medicoRepositoryPort.findById(medicoId)).thenReturn(Optional.of(medico));

        mockMvc.perform(get("/api/v1/pacientes/{pacienteId}/citas", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
    }

    @Test
    void given_unknownPaciente_when_getCitas_then_404() throws Exception {
        when(consultarCitasPacienteUseCase.execute(any()))
                .thenThrow(new NoSuchElementException("Paciente no encontrado"));

        mockMvc.perform(get("/api/v1/pacientes/{pacienteId}/citas", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
