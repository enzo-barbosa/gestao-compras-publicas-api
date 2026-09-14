package com.gestaocompras.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String remetente;

    public EmailService(
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.resend.from:onboarding@resend.dev}") String remetente) {
        this.restClient = RestClient.builder().baseUrl("https://api.resend.com/emails").build();
        this.apiKey = apiKey;
        this.remetente = remetente;
    }

    public void enviarCodigoRecuperacao(String email, String codigo) {
        if (apiKey.isBlank()) {
            log.warn("[recuperacao-senha] Sem RESEND_API_KEY configurada: "
                    + "código para {} = {}", email, codigo);
            return;
        }
        try {
            restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", remetente,
                            "to", List.of(email),
                            "subject", "Recuperação de senha — Gestão de Compras Públicas",
                            "text", "Seu código de recuperação de senha é: " + codigo
                                    + ". Ele expira em 15 minutos."))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de recuperação via Resend para {}: {}",
                    email, e.getMessage());
        }
    }
}