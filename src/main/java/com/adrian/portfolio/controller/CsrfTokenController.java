package com.adrian.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Entrega la cookie {@code XSRF-TOKEN} al visitante que aún no la tiene: el
 * frontend la necesita antes de su primera llamada a {@code /api/projects}, y
 * solo la emite una respuesta del backend ({@code CsrfCookieFilter}).
 *
 * <p>Sin cuerpo y sin tocar la sesión. Antes generaba un UUID y lo guardaba en la
 * {@code WebSession}, lo que hacía de este endpoint —público y sin cupo— una
 * fábrica de sesiones: con el almacén lleno (10.000) todo visitante recibía un
 * 500. Lo cubre {@code CsrfFlowIntegrationTest}.
 */
@RestController
@RequestMapping("/api/csrf-token")
public class CsrfTokenController {

    @GetMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> getCsrfToken() {
        return Mono.empty();
    }
}
