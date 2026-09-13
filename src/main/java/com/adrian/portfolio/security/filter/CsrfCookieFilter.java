package com.adrian.portfolio.security.filter;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * El {@code Mono<CsrfToken>} que Spring Security deja en el exchange es perezoso:
 * la cookie XSRF-TOKEN no se escribe hasta que alguien se suscribe, y aquí no hay
 * plantilla de servidor que lo haga. Sin este filtro el navegador nunca recibiría
 * el token y el POST del chat sería un 403 seguro.
 *
 * <p>Solo se fuerza en {@code /api/**} por dos motivos: son las únicas respuestas
 * que salen con {@code no-store} (ver CacheControlFilter), así que emitir la
 * cookie junto a un asset {@code immutable} dejaría que una caché compartida
 * sirviera el mismo token a todos los visitantes; y el frontend ya llama a
 * {@code /api/csrf-token} al cargar la página, así que la cookie está puesta
 * mucho antes de que nadie abra el chat.
 *
 * <p>No lleva {@code @Order}: tiene que ejecutarse por detrás del
 * WebFilterChainProxy de Spring Security (orden -100), que es quien deja el
 * atributo en el exchange.
 */
@Component
public class CsrfCookieFilter implements WebFilter {

    private static final String API_PREFIX = "/api/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getURI().getPath().startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }

        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        return csrfToken == null
                ? chain.filter(exchange)
                : csrfToken.then(chain.filter(exchange));
    }
}
