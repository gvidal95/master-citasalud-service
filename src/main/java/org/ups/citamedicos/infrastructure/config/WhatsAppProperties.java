package org.ups.citamedicos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private String apiUrl;
    private String bearerToken;
    private String phoneNumberId;

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
}
