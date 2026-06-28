package org.ups.citamedicos.integration.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.ups.citamedicos.adapter.in.web.CitaController;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
class CitaControllerPostMvcTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void given_validRequest_when_postCitas_then_201_withCitaResponse() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        UUID franjaId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();

        LocalDateTime franjaInicio = LocalDateTime.now().plusDays(5);
        Cita cita = Cita.nueva(pacienteId, franjaId, UUID.randomUUID().toString(), franjaInicio);
        FranjaHoraria franja = new FranjaHoraria(franjaId, medicoId, LocalDate.now().plusDays(5),
                LocalTime.of(10, 0), LocalTime.of(10, 30), EstadoFranja.OCUPADA, 1L);
        Medico medico = new Medico(medicoId, "Dr. García", "Cardiología");

        when(reservarCitaUseCase.execute(any())).thenReturn(cita);
        when(franjaHorariaRepositoryPort.findById(franjaId)).thenReturn(Optional.of(franja));
        when(medicoRepositoryPort.findById(medicoId)).thenReturn(Optional.of(medico));

        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);

        mockMvc.perform(post("/api/v1/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void given_franjaOcupada_when_postCitas_then_409() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        UUID franjaId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();

        when(reservarCitaUseCase.execute(any()))
                .thenThrow(new FranjaNoDisponibleException(franjaId, medicoId));

        Map<String, Object> request = Map.of("pacienteId", pacienteId, "franjaHorariaId", franjaId);

        mockMvc.perform(post("/api/v1/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }
}
