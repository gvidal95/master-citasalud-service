package org.ups.citamedicos.unit.domain;

import org.junit.jupiter.api.Test;
import org.ups.citamedicos.domain.valueobject.NumeroWhatsApp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroWhatsAppTest {

    @Test
    void given_e164Format_when_create_then_accepted() {
        NumeroWhatsApp numero = NumeroWhatsApp.of("+593987654321");
        assertThat(numero.getValue()).isEqualTo("+593987654321");
    }

    @Test
    void given_noPlus_when_create_then_throwsIllegalArgument() {
        assertThatThrownBy(() -> NumeroWhatsApp.of("593987654321"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_tooShort_when_create_then_throwsIllegalArgument() {
        assertThatThrownBy(() -> NumeroWhatsApp.of("+1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_nullValue_when_create_then_throwsIllegalArgument() {
        assertThatThrownBy(() -> NumeroWhatsApp.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_sameValue_when_equals_then_true() {
        NumeroWhatsApp a = NumeroWhatsApp.of("+593987654321");
        NumeroWhatsApp b = NumeroWhatsApp.of("+593987654321");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void given_differentValue_when_equals_then_false() {
        NumeroWhatsApp a = NumeroWhatsApp.of("+593987654321");
        NumeroWhatsApp b = NumeroWhatsApp.of("+573001234567");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void given_sameInstance_when_equals_then_true() {
        NumeroWhatsApp a = NumeroWhatsApp.of("+593987654321");
        assertThat(a).isEqualTo(a);
    }

    @Test
    void given_nonNumeroWhatsApp_when_equals_then_false() {
        NumeroWhatsApp a = NumeroWhatsApp.of("+593987654321");
        assertThat(a).isNotEqualTo("+593987654321");
    }

    @Test
    void given_valid_when_toString_then_returnsValue() {
        NumeroWhatsApp a = NumeroWhatsApp.of("+593987654321");
        assertThat(a.toString()).isEqualTo("+593987654321");
    }
}
