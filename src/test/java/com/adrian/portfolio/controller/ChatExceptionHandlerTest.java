package com.adrian.portfolio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.github.adrian0511.prompt_link.exceptions.AiClientException;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class ChatExceptionHandlerTest {

    private final ChatExceptionHandler handler =
            new ChatExceptionHandler("google/gemma-4-31b-it:free", "openrouter/free");

    private String bodyFor(int statusCode) {
        ResponseEntity<Flux<String>> response =
                handler.handleAiClientException(new AiClientException("fallo", statusCode, null));
        return String.join("", response.getBody().collectList().block());
    }

    @Test
    void siFaltaLaApiKeyDerivaAlEmailEnLugarDeFallar() {
        assertThat(bodyFor(AiClientException.CONFIGURATION_ERROR)).contains("adriangarces0310@gmail.com");
    }

    @Test
    void distingueElLimiteDeConsultas() {
        assertThat(bodyFor(429)).contains("límite de consultas");
    }

    @Test
    void distingueLaFaltaDeCredito() {
        assertThat(bodyFor(402)).contains("no está disponible");
    }

    @Test
    void unFalloDeRedTambienDaTextoUtil() {
        assertThat(bodyFor(AiClientException.NETWORK_ERROR)).contains("adriangarces0310@gmail.com");
    }

    /**
     * Un modelo que razona puede gastarse los tokens sin escribir nada. Hasta la 1.2.0
     * de prompt-link eso era un stream vacío y el visitante veía una burbuja en blanco;
     * ahora llega como INVALID_RESPONSE y tiene que acabar en una frase útil.
     */
    @Test
    void unaRespuestaVaciaDelModeloTambienDaTextoUtil() {
        assertThat(bodyFor(AiClientException.INVALID_RESPONSE)).contains("adriangarces0310@gmail.com");
    }

    @Test
    void respondeComoStreamY200ParaQueElChatLoPinteComoUnaRespuestaMas() {
        ResponseEntity<Flux<String>> response =
                handler.handleAiClientException(new AiClientException("x", 500, null));

        // Un 500 dejaría al visitante viendo un error; aquí ve una frase útil.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
    }
}
