package com.adrian.portfolio.security.filter;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * El {@code Mono<CsrfToken>} que Spring Security deja en el exchange es perezoso:
 * sin alguien que se suscriba, la cookie {@code XSRF-TOKEN} no se escribe nunca y
 * el POST del chat sería un 403 seguro. Aquí no hay plantilla de servidor que lo
 * haga, así que lo hace este filtro.
 *
 * <p>Solo en {@code /api/**}: son las únicas respuestas {@code no-store}, y emitir
 * la cookie junto a un asset {@code immutable} dejaría que una caché compartida
 * sirviera el mismo token a todos los visitantes.
 *
 * <p>Sin {@code @Order} —el último— porque tiene que correr por detrás del
 * {@code WebFilterChainProxy} (orden -100), que es quien pone el atributo.
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
