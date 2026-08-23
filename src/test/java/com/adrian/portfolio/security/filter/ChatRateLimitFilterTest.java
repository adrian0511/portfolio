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

    private static final int SIN_LIMITE = 10_000;

    private final AtomicInteger reachedChain = new AtomicInteger();

    private final WebFilterChain chain = exchange -> {
        reachedChain.incrementAndGet();
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return exchange.getResponse().setComplete();
    };

    private WebSession newSession() {
        return new InMemoryWebSessionStore().createWebSession().block();
    }

    /** Cada petición trae su propia respuesta; lo que comparten es sesión e IP. */
    private MockServerWebExchange chatRequest(WebSession session, String ip) {
        return MockServerWebExchange.builder(
                MockServerHttpRequest.post("/api/chat").remoteAddress(
                        new java.net.InetSocketAddress(ip, 12345)))
                .session(session)
                .build();
    }

    @Test
    void dejaPasarMientrasNoSeAgoteElCupoDeLaSesion() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(3, SIN_LIMITE, SIN_LIMITE);
        WebSession session = newSession();

        for (int i = 0; i < 3; i++) {
            MockServerWebExchange exchange = chatRequest(session, "10.0.0.1");
            filter.filter(exchange, chain).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        assertThat(reachedChain.get()).isEqualTo(3);
    }

    @Test
    void alSuperarElCupoDeSesionDevuelve429YNoLlamaAlModelo() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(2, SIN_LIMITE, SIN_LIMITE);
        WebSession session = newSession();

        filter.filter(chatRequest(session, "10.0.0.1"), chain).block();
        filter.filter(chatRequest(session, "10.0.0.1"), chain).block();
        MockServerWebExchange rejected = chatRequest(session, "10.0.0.1");
        filter.filter(rejected, chain).block();

        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void elCupoPorIpFrenaAQuienDescartaLaCookieParaRenovarSesion() {
        // El ataque real: crear una sesión nueva cuesta una petición, así que el
        // cupo de sesión por sí solo no protege nada. La IP sí es escasa.
        ChatRateLimitFilter filter = new ChatRateLimitFilter(SIN_LIMITE, 3, SIN_LIMITE);

        for (int i = 0; i < 3; i++) {
            filter.filter(chatRequest(newSession(), "203.0.113.7"), chain).block();
        }

        MockServerWebExchange rejected = chatRequest(newSession(), "203.0.113.7");
        filter.filter(rejected, chain).block();

        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(reachedChain.get()).isEqualTo(3);
    }

    @Test
    void ipsDistintasNoCompartenCupo() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(SIN_LIMITE, 1, SIN_LIMITE);

        filter.filter(chatRequest(newSession(), "203.0.113.7"), chain).block();
        filter.filter(chatRequest(newSession(), "198.51.100.9"), chain).block();

        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void elTopeDiarioGlobalAcotaElGastoPaseLoQuePase() {
        // Última línea de defensa: aunque el atacante tenga muchas IPs, la cuota
        // gratuita no se puede vaciar más allá de este tope.
        ChatRateLimitFilter filter = new ChatRateLimitFilter(SIN_LIMITE, SIN_LIMITE, 2);

        filter.filter(chatRequest(newSession(), "203.0.113.1"), chain).block();
        filter.filter(chatRequest(newSession(), "203.0.113.2"), chain).block();
        MockServerWebExchange rejected = chatRequest(newSession(), "203.0.113.3");
        filter.filter(rejected, chain).block();

        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void funcionaConDireccionesSinResolver() {
        // Detrás del proxy de Railway, Spring construye la dirección a partir de
        // X-Forwarded-For con InetSocketAddress.createUnresolved(...), y en ese
        // caso getAddress() devuelve null. Usarlo sin comprobarlo tumbaba el
        // chat en producción con un 500 en cada petición.
        ChatRateLimitFilter filter = new ChatRateLimitFilter(SIN_LIMITE, 2, SIN_LIMITE);

        MockServerWebExchange first = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/api/chat")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("203.0.113.7", 443)))
                .session(newSession()).build();

        filter.filter(first, chain).block();

        assertThat(first.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reachedChain.get()).isEqualTo(1);
    }

    @Test
    void elCupoPorIpTambienCuentaConDireccionesSinResolver() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(SIN_LIMITE, 2, SIN_LIMITE);

        for (int i = 0; i < 2; i++) {
            filter.filter(MockServerWebExchange.builder(
                    MockServerHttpRequest.post("/api/chat")
                            .remoteAddress(java.net.InetSocketAddress.createUnresolved("203.0.113.7", 443)))
                    .session(newSession()).build(), chain).block();
        }

        MockServerWebExchange rejected = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/api/chat")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("203.0.113.7", 443)))
                .session(newSession()).build();
        filter.filter(rejected, chain).block();

        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(reachedChain.get()).isEqualTo(2);
    }

    @Test
    void otrasRutasNoConsumenCupo() {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(1, 1, 1);
        WebSession session = newSession();

        for (int i = 0; i < 3; i++) {
            filter.filter(MockServerWebExchange.builder(MockServerHttpRequest.get("/api/projects"))
                    .session(session).build(), chain).block();
        }

        assertThat(reachedChain.get()).isEqualTo(3);
    }
}
