package com.adrian.portfolio.security.filter;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.InMemoryWebSessionStore;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRateLimitFilterTest {

    private final AtomicInteger reachedChain = new AtomicInteger();

    private final WebFilterChain chain = exchange -> {
        reachedChain.incrementAndGet();
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return exchange.getResponse().setComplete();
    };

    private WebSession newSession() {
        return new InMemoryWebSessionStore().createWebSession().block();
    }

    /** Cada petición trae su propia respuesta; lo que comparten es la sesión. */
    private MockServerWebExchange chatRequest(WebSession session) {
        return MockServerWebExchange.builder(MockServerHttpRequest.post("/api/chat"))
                .session(session)
                .build();
    }

    @Test
    void dejaPasarMientrasNoSeAgoteElCupoDeLaSesion() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(3);
        WebSession session = newSession();

        for (int i = 0; i < 3; i++) {
            MockServerWebExchange exchange = chatRequest(session);
            filter.filter(exchange, chain).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        assertThat(reachedChain.get()).isEqualTo(3);
    }

    @Test
    void alSuperarElCupoDevuelve429YNoLlamaAlModelo() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(2);
        WebSession session = newSession();

        filter.filter(chatRequest(session), chain).block();
        filter.filter(chatRequest(session), chain).block();

        MockServerWebExchange rejected = chatRequest(session);
        filter.filter(rejected, chain).block();

        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        // La de más no debe llegar al chain: cada llamada al modelo cuesta dinero.
        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void elCupoEsPorSesionYNoGlobal() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(1);

        filter.filter(chatRequest(newSession()), chain).block();
        filter.filter(chatRequest(newSession()), chain).block();

        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void otrasRutasNoConsumenCupo() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(1);
        WebSession session = newSession();

        for (int i = 0; i < 3; i++) {
            filter.filter(MockServerWebExchange.builder(MockServerHttpRequest.get("/api/projects"))
                    .session(session).build(), chain).block();
        }

        assertThat(reachedChain.get()).isEqualTo(3);
    }
}
