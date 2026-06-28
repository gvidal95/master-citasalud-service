package org.ups.citamedicos.integration.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ups.citamedicos.adapter.in.web.MedicoController;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicoController.class)
class MedicoControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarMedicosUseCase buscarMedicosUseCase;

    @MockitoBean
    private ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

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

    @Test
    void given_validRequest_when_getMedicos_then_200_withList() throws Exception {
        Medico medico = new Medico(UUID.randomUUID(), "Dr. García", "Cardiología");
        when(buscarMedicosUseCase.execute(any())).thenReturn(List.of(medico));

        mockMvc.perform(get("/api/v1/medicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Dr. García"));
    }

    @Test
    void given_validMedicoId_when_getDisponibilidad_then_200_onlyFranjasLibres() throws Exception {
        UUID medicoId = UUID.randomUUID();
        FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId,
                LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(9, 30),
                EstadoFranja.LIBRE, 0L);
        when(consultarDisponibilidadUseCase.execute(any())).thenReturn(List.of(franja));

        mockMvc.perform(get("/api/v1/medicos/{medicoId}/disponibilidad", medicoId)
                        .param("fechaDesde", LocalDate.now().toString())
                        .param("fechaHasta", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("LIBRE"));
    }

    @Test
    void given_unknownMedicoId_when_getDisponibilidad_then_404() throws Exception {
        when(consultarDisponibilidadUseCase.execute(any()))
                .thenThrow(new NoSuchElementException("Médico no encontrado"));

        mockMvc.perform(get("/api/v1/medicos/{medicoId}/disponibilidad", UUID.randomUUID())
                        .param("fechaDesde", LocalDate.now().toString())
                        .param("fechaHasta", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isNotFound());
    }
}
