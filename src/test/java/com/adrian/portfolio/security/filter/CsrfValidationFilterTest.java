package com.adrian.portfolio.security.filter;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfValidationFilterTest {

    private static final String CSRF_TOKEN_ATTR = "CSRF_TOKEN";

    private final CsrfValidationFilter filter = new CsrfValidationFilter();

    private WebFilterChain chainRecording(AtomicBoolean chainCalled) {
        return exchange -> {
            chainCalled.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }

    @Test
    void sinHeaderCsrfDevuelve404YNoLlamaAlChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/projects"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainRecording(chainCalled)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void conHeaderQueNoCoincideConLaSesionDevuelve404YNoLlamaAlChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/projects").header("X-CSRF-Token", "token-invalido"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainRecording(chainCalled)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void conHeaderQueCoincideConElTokenDeSesionDejaPasarLaPeticion() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/projects").header("X-CSRF-Token", "token-valido"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        exchange.getSession()
                .doOnNext(session -> session.getAttributes().put(CSRF_TOKEN_ATTR, "token-valido"))
                .flatMap(session -> filter.filter(exchange, chainRecording(chainCalled)))
                .block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rutaDistintaDeProjectsIgnoraLaValidacionCsrf() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/otra-cosa"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainRecording(chainCalled)).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }
}
