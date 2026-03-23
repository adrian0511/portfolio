package com.adrian.portfolio.controller;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/csrf-token")
public class CsrfTokenController {
    private static final String CSRF_TOKEN_ATTR = "CSRF_TOKEN";

    @GetMapping
    public Mono<Map<String, String>> getCsrfToken(ServerWebExchange exchange) {
        return exchange.getSession()
                .map(session -> {
                    String token = session.getAttribute(CSRF_TOKEN_ATTR);

                    if (token == null) {
                        token = UUID.randomUUID().toString();
                        session.getAttributes().put(CSRF_TOKEN_ATTR, token);
                    }

                    return Collections.singletonMap("token", token);
                });
    }
}
