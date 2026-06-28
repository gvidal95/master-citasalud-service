package org.ups.citamedicos.unit.domain;

import org.junit.jupiter.api.Test;
import org.ups.citamedicos.domain.exception.FranjaNoDisponibleException;
import org.ups.citamedicos.domain.model.FranjaHoraria;
import org.ups.citamedicos.domain.valueobject.EstadoFranja;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FranjaHorariaTest {

    private FranjaHoraria franjaLibre() {
        return new FranjaHoraria(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(3),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                EstadoFranja.LIBRE, 0L);
    }

    private FranjaHoraria franjaOcupada() {
        return new FranjaHoraria(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(3),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                EstadoFranja.OCUPADA, 1L);
    }

    @Test
    void given_franjaLibre_when_reservar_then_estadoOcupada() {
        FranjaHoraria franja = franjaLibre();
        franja.reservar();
        assertThat(franja.getEstado()).isEqualTo(EstadoFranja.OCUPADA);
    }

    @Test
    void given_franjaOcupada_when_reservar_then_throwsFranjaNoDisponibleException() {
        FranjaHoraria franja = franjaOcupada();
        assertThatThrownBy(franja::reservar)
                .isInstanceOf(FranjaNoDisponibleException.class);
    }

    @Test
    void given_franjaOcupada_when_liberar_then_estadoLibre() {
        FranjaHoraria franja = franjaOcupada();
        franja.liberar();
        assertThat(franja.getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }
}
