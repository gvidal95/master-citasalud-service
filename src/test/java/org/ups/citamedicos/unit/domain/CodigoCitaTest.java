package org.ups.citamedicos.unit.domain;

import org.junit.jupiter.api.Test;
import org.ups.citamedicos.domain.valueobject.CodigoCita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoCitaTest {

    @Test
    void given_generate_when_called_then_validUUIDv4() {
        CodigoCita codigo = CodigoCita.generate();
        assertThat(codigo.getValue())
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    @Test
    void given_invalidString_when_createFromValue_then_throwsIllegalArgument() {
        assertThatThrownBy(() -> CodigoCita.of("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_validUUIDv4String_when_of_then_accepted() {
        String uuidV4 = "550e8400-e29b-41d4-a716-446655440000";
        CodigoCita codigo = CodigoCita.of(uuidV4);
        assertThat(codigo.getValue()).isEqualTo(uuidV4);
    }

    @Test
    void given_generatedCodigo_when_getValue_then_sameValue() {
        CodigoCita codigo = CodigoCita.generate();
        CodigoCita same = CodigoCita.of(codigo.getValue());
        assertThat(same).isEqualTo(codigo);
    }
}
