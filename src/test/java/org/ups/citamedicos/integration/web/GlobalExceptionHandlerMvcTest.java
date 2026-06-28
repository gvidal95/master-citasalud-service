package org.ups.citamedicos.integration.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ups.citamedicos.adapter.in.web.CitaController;
import org.ups.citamedicos.application.port.in.BuscarMedicosUseCase;
import org.ups.citamedicos.application.port.in.CancelarCitaUseCase;
import org.ups.citamedicos.application.port.in.ConsultarCitasPacienteUseCase;
import org.ups.citamedicos.application.port.in.ConsultarDisponibilidadUseCase;
import org.ups.citamedicos.application.port.in.ReservarCitaUseCase;
import org.ups.citamedicos.application.port.out.FranjaHorariaRepositoryPort;
import org.ups.citamedicos.application.port.out.MedicoRepositoryPort;
import org.ups.citamedicos.domain.exception.CitaDuplicadaException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
class GlobalExceptionHandlerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ReservarCitaUseCase reservarCitaUseCase;
    @MockitoBean private CancelarCitaUseCase cancelarCitaUseCase;
    @MockitoBean private ConsultarCitasPacienteUseCase consultarCitasPacienteUseCase;
    @MockitoBean private MedicoRepositoryPort medicoRepositoryPort;
    @MockitoBean private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @MockitoBean private ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    @Test
    void given_citaDuplicada_when_post_then_422() throws Exception {
        when(reservarCitaUseCase.execute(any()))
                .thenThrow(new CitaDuplicadaException("some-idempotency-key"));

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/citas")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + UUID.randomUUID() + "\",\"franjaHorariaId\":\"" + UUID.randomUUID() + "\"}")
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CITA_DUPLICADA"));
    }

    @Test
    void given_illegalState_when_cancelar_then_409() throws Exception {
        doThrow(new IllegalStateException("Cita ya cancelada"))
                .when(cancelarCitaUseCase).execute(any());

        mockMvc.perform(delete("/api/v1/citas/{codigo}", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
    }

    @Test
    void given_illegalArgument_when_getCitasPaciente_then_400() throws Exception {
        when(consultarCitasPacienteUseCase.execute(any()))
                .thenThrow(new IllegalArgumentException("Paciente inválido"));

        mockMvc.perform(get("/api/v1/pacientes/{pacienteId}/citas", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void given_genericException_when_cancelar_then_500() throws Exception {
        doThrow(new RuntimeException("error inesperado"))
                .when(cancelarCitaUseCase).execute(any());

        mockMvc.perform(delete("/api/v1/citas/{codigo}", UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));
    }
}
