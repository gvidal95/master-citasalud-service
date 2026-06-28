package org.ups.citamedicos.application.port.in;

import org.ups.citamedicos.domain.valueobject.CodigoCita;

public interface CancelarCitaUseCase {

    record CancelarCitaCommand(CodigoCita codigoCita) {}

    void execute(CancelarCitaCommand command);
}
