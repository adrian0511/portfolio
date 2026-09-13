package com.adrian.portfolio.security.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Portero de {@code GET /api/projects}: la cabecera tiene que traer el mismo
 * token que la cookie {@code XSRF-TOKEN}.
 *
 * <p><b>No es protección CSRF</b>, y por eso no lo hace el mecanismo nativo: no
 * hay sesión autenticada que forjar, y además este es un GET, que Spring Security
 * ignora por diseño. Es una barrera blanda contra llamadas directas y scraping —
 * descartar la cookie solo cuesta una petición más, de ahí que el chat lleve
 * además cupos de uso. El POST del chat sí va por el CSRF nativo.
 *
 * <p>Se valida contra la cookie y no contra la {@code WebSession}: guardar el
 * token en sesión convertía cada petición en estado de servidor, y con el
 * almacén lleno (10.000) la web respondía 500 a todo el mundo.
 *
 * <p>Responde 404 y no 403 para no confirmar que el endpoint existe.
 */
@Component
@Order(-100)
public class CsrfValidationFilter implements WebFilter {

    private static final String PROTECTED_PATH = "/api/projects";
    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String HEADER_NAME = "X-XSRF-TOKEN";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!PROTECTED_PATH.equals(exchange.getRequest().getURI().getPath())) {
            return chain.filter(exchange);
        }

        String fromHeader = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_NAME);
        String fromCookie = cookie == null ? null : cookie.getValue();

        if (fromHeader == null || fromCookie == null || !matches(fromHeader, fromCookie)) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    // En tiempo constante: comparar tokens con equals() filtra por cuánto tardan
    // en diferir. Aquí el riesgo es teórico, pero no cuesta nada hacerlo bien.
    private boolean matches(String header, String cookie) {
        return MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8),
                cookie.getBytes(StandardCharsets.UTF_8));
    }

}
