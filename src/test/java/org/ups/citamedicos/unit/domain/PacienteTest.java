package org.ups.citamedicos.unit.domain;

import org.junit.jupiter.api.Test;
import org.ups.citamedicos.domain.model.Paciente;
import org.ups.citamedicos.domain.valueobject.NumeroWhatsApp;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PacienteTest {

    @Test
    void given_paciente_when_getters_then_returnsCorrectValues() {
        UUID id = UUID.randomUUID();
        NumeroWhatsApp numero = NumeroWhatsApp.of("+573001234567");
        Paciente paciente = new Paciente(id, "Carlos Mendoza", numero);

        assertThat(paciente.getId()).isEqualTo(id);
        assertThat(paciente.getNombre()).isEqualTo("Carlos Mendoza");
        assertThat(paciente.getNumeroWhatsApp()).isEqualTo(numero);
        assertThat(paciente.getNumeroWhatsApp().getValue()).isEqualTo("+573001234567");
    }
}
