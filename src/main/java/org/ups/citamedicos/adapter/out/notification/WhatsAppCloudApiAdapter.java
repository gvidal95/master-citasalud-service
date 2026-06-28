package org.ups.citamedicos.adapter.out.notification;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;
import org.ups.citamedicos.application.port.out.NotificacionWhatsAppPort;
import org.ups.citamedicos.domain.model.Cita;
import org.ups.citamedicos.domain.model.Medico;
import org.ups.citamedicos.domain.model.Notificacion;
import org.ups.citamedicos.domain.model.Paciente;
import org.ups.citamedicos.infrastructure.config.WhatsAppProperties;

import java.util.Map;

public class WhatsAppCloudApiAdapter implements NotificacionWhatsAppPort {

    private final WhatsAppProperties properties;
    private final RestClient restClient;

    public WhatsAppCloudApiAdapter(WhatsAppProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .build();
    }

    @Override
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void enviar(Notificacion notificacion, Paciente paciente, Cita cita, Medico medico) {
        String texto = String.format(
                "Cita confirmada con %s. Código: %s",
                medico.getNombre(),
                cita.getCodigo().getValue()
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", paciente.getNumeroWhatsApp().getValue(),
                "type", "text",
                "text", Map.of("body", texto)
        );

        restClient.post()
                .uri("/{phoneNumberId}/messages", properties.getPhoneNumberId())
                .header("Authorization", "Bearer " + properties.getBearerToken())
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
