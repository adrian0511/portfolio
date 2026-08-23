package com.adrian.portfolio.security.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Un endpoint de IA público sin límite es una factura abierta: cualquiera puede
 * dejarlo en bucle. El contador vive en la sesión, la misma que ya sostiene el
 * flujo CSRF.
 */
@Component
@Order(-95)
public class ChatRateLimitFilter implements WebFilter {

    private static final String CHAT_PATH = "/api/chat";
    private static final String COUNT_ATTR = "CHAT_COUNT";

    private final int maxMessages;

    public ChatRateLimitFilter(@Value("${chat.max-messages-per-session:20}") int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!CHAT_PATH.equals(exchange.getRequest().getURI().getPath())) {
            return chain.filter(exchange);
        }

        return exchange.getSession().flatMap(session -> {
            int used = session.getAttributeOrDefault(COUNT_ATTR, 0);
            if (used >= maxMessages) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }

            session.getAttributes().put(COUNT_ATTR, used + 1);
            return chain.filter(exchange);
        });
    }
}
