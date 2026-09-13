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
 * Portero de {@code GET /api/projects}: exige que la cabecera traiga el mismo
 * token que la cookie {@code XSRF-TOKEN} (double-submit, igual que el chat).
 *
 * <p><b>Por qué no lo hace el CSRF de Spring Security.</b> Este es un GET, y el
 * mecanismo nativo ignora por diseño los métodos seguros (GET, HEAD, OPTIONS,
 * TRACE): configurado, dejaría pasar la petición siempre, y no hay opción para
 * cambiarlo. El POST del chat sí va por el nativo, que es donde encaja.
 *
 * <p><b>Por qué contra la cookie y no contra la sesión.</b> Antes el token se
 * guardaba en la {@code WebSession}, así que pedirlo creaba estado en servidor:
 * unas miles de peticiones llenaban el almacén de sesiones y la web respondía
 * 500 a todo el mundo. Validar contra la cookie que Spring Security ya emite
 * da el mismo portero —siguen haciendo falta cookie y cabecera— con cero
 * estado. De paso, en toda la app hay un único token.
 *
 * <p><b>Y qué es esto en realidad.</b> No es protección CSRF: no hay sesión
 * autenticada ni efecto de lado que un tercero pueda forjar leyendo repos
 * públicos. Es un portero blando contra llamadas directas y scraping. Como
 * barrera es débil a propósito: descartar la cookie solo cuesta una petición
 * más, por eso el chat tiene además cupos de uso.
 *
 * <p>Responde 404 y no 403 para no confirmar siquiera que el endpoint existe.
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
