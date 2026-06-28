package org.ups.citamedicos.unit.domain;

import org.junit.jupiter.api.Test;
import org.ups.citamedicos.domain.exception.CancelacionFueraDePlazoException;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.valueobject.EstadoCita;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitaTest {

    @Test
    void given_validData_when_nueva_then_estadoConfirmada_andCodigoNotNull() {
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(),
                "idem-key-1", LocalDateTime.now().plusDays(5));
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(cita.getCodigo()).isNotNull();
    }

    @Test
    void given_citaConfirmada_andMoreThan24h_when_cancelar_then_estadoCancelada() {
        LocalDateTime franjaInicio = LocalDateTime.now(ZoneOffset.UTC).plusHours(48);
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), "idem-2", franjaInicio);
        cita.cancelar(Instant.now());
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void given_citaConfirmada_andLessThan24h_when_cancelar_then_throwsCancelacionFueraDePlazoException() {
        LocalDateTime franjaInicio = LocalDateTime.now(ZoneOffset.UTC).plusHours(12);
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), "idem-3", franjaInicio);
        assertThatThrownBy(() -> cita.cancelar(Instant.now()))
                .isInstanceOf(CancelacionFueraDePlazoException.class);
    }
}
