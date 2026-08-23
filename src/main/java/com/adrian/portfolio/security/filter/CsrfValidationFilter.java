package com.adrian.portfolio.security.filter;

import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(-100)
public class CsrfValidationFilter implements WebFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/api/projects", "/api/chat");
    private static final String CSRF_TOKEN_ATTR = "CSRF_TOKEN";
    private static final String HEADER_NAME = "X-CSRF-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!PROTECTED_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        String providerToken = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);

        if (providerToken == null) {
            return sendNotFound(exchange);
        }

        return exchange.getSession()
                .flatMap(session -> {
                    String sessionToken = session.getAttribute(CSRF_TOKEN_ATTR);

                    if (sessionToken != null && sessionToken.equals(providerToken)) {
                        return chain.filter(exchange);
                    } else {
                        return sendNotFound(exchange);
                    }
                });

    }

    private Mono<Void> sendNotFound(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return exchange.getResponse().setComplete();
    }

}
