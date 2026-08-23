package com.adrian.portfolio.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.adrian0511.prompt_link.exceptions.AiClientException;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

/**
 * El resto de la web nunca rompe (los proyectos tienen lista de respaldo) y el
 * chat no es la excepción: cualquier fallo del modelo se traduce a texto útil
 * para el visitante en vez de propagarse como error.
 */
@RestControllerAdvice
@Log4j2
public class ChatExceptionHandler {

    private static final String CONTACT = "adriangarces0310@gmail.com";

    @ExceptionHandler(AiClientException.class)
    public ResponseEntity<Flux<String>> handleAiClientException(AiClientException error) {
        log(error);

        // 200 a propósito: para el visitante no es un error, es una respuesta más.
        // El detalle real queda en el log, no en la pantalla de quien pregunta.
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.just(messageFor(error.getStatusCode())));
    }

    private void log(AiClientException error) {
        if (error.getStatusCode() == AiClientException.CONFIGURATION_ERROR) {
            log.warn("Chat sin configurar: falta ai.api-key");
        } else {
            log.error("Fallo del modelo (statusCode={}): {}", error.getStatusCode(), error.getMessage());
        }
    }

    private String messageFor(int statusCode) {
        return switch (statusCode) {
            case 429 -> "Ahora mismo he alcanzado el límite de consultas. "
                    + "Prueba en un rato o escribe a " + CONTACT + ".";
            case 402 -> "El asistente no está disponible por ahora. "
                    + "Puedes escribir a " + CONTACT + " y Adrián te contestará.";
            default -> "Ahora mismo no puedo responder. "
                    + "Puedes escribir a " + CONTACT + " y Adrián te contestará.";
        };
    }
}
