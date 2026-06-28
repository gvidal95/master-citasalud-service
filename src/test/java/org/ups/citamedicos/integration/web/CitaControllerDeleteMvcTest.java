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
import org.ups.citamedicos.domain.exception.CancelacionFueraDePlazoException;
import org.ups.citamedicos.domain.valueobject.CodigoCita;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
class CitaControllerDeleteMvcTest {

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
    void given_validCodigo_when_delete_then_204() throws Exception {
        doNothing().when(cancelarCitaUseCase).execute(any());
        String codigoUuid = "550e8400-e29b-4" + "1d4-a716-446655440000".replace("a", "b");
        CodigoCita codigo = CodigoCita.generate();

        mockMvc.perform(delete("/api/v1/citas/{codigoCita}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void given_citaFueraDePlazo_when_delete_then_400() throws Exception {
        CodigoCita codigo = CodigoCita.generate();
        doThrow(new CancelacionFueraDePlazoException(codigo))
                .when(cancelarCitaUseCase).execute(any());

        mockMvc.perform(delete("/api/v1/citas/{codigoCita}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_unknownCodigo_when_delete_then_404() throws Exception {
        doThrow(new NoSuchElementException("Cita no encontrada"))
                .when(cancelarCitaUseCase).execute(any());

        mockMvc.perform(delete("/api/v1/citas/{codigoCita}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
