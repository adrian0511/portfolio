package com.adrian.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Entrega la cookie {@code XSRF-TOKEN} al visitante que aún no la tiene: el
 * frontend necesita el token antes de su primera llamada a {@code /api/projects},
 * y la cookie solo la emite una respuesta del backend.
 *
 * <p>No devuelve cuerpo ni toca la sesión. <b>Antes sí</b>: generaba un UUID y lo
 * guardaba en la {@code WebSession}, lo que convertía este endpoint — público, sin
 * cupo y sin necesidad de cookie — en una fábrica de sesiones. Con el tope de
 * {@code InMemoryWebSessionStore} (10.000) bastaban unos miles de peticiones para
 * que el almacén se llenara y <b>todo visitante recibiera un 500</b>. Ahora el
 * token vive en la cookie y el servidor no guarda nada.
 *
 * <p>Quien escribe la cookie es {@code CsrfCookieFilter}, que se suscribe al
 * token diferido de Spring Security en cualquier respuesta a {@code /api/**}.
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
