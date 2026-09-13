package com.adrian.portfolio.security.filter;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfValidationFilterTest {

    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";

    private final CsrfValidationFilter filter = new CsrfValidationFilter();
    private final AtomicBoolean chainCalled = new AtomicBoolean(false);

    private final WebFilterChain chain = exchange -> {
        chainCalled.set(true);
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return Mono.empty();
    };

    private ServerWebExchange projectsRequest(String cookie, String header) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/projects");
        if (cookie != null) {
            request = request.cookie(new HttpCookie(COOKIE, cookie));
        }
        if (header != null) {
            request = request.header(HEADER, header);
        }
        return MockServerWebExchange.from(request);
    }

    @Test
    void conCookieYCabeceraCoincidentesDejaPasarLaPeticion() {
        ServerWebExchange exchange = projectsRequest("token-valido", "token-valido");

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sinCabeceraDevuelve404YNoLlamaAlChain() {
        ServerWebExchange exchange = projectsRequest("token-valido", null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void sinCookieDevuelve404AunqueLaCabeceraVenga() {
        ServerWebExchange exchange = projectsRequest(null, "token-valido");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void conCabeceraQueNoCoincideConLaCookieDevuelve404() {
        ServerWebExchange exchange = projectsRequest("token-valido", "otro-token");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void elChatNoPasaPorEsteFiltroSinoPorElCsrfNativo() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/chat"));

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rutaDistintaDeProjectsIgnoraLaValidacion() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/otra-cosa"));

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }
}
