package com.adrian.portfolio.security.config;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedClientIpTransformerTest {

    private static final InetSocketAddress PROXY = new InetSocketAddress("10.0.0.1", 40000);

    private final TrustedClientIpTransformer transformer = new TrustedClientIpTransformer();

    private String clientIpOf(MockServerHttpRequest.BaseBuilder<?> builder) {
        ServerHttpRequest request = transformer.apply(builder.remoteAddress(PROXY).build());
        return request.getRemoteAddress() == null ? null : request.getRemoteAddress().getHostString();
    }

    @Test
    void conIpFalsaDelanteSeQuedaConLaQueAnadeElProxy() {
        // Lo que produce Railway cuando el cliente manda su propia cabecera:
        // la real va detrás, y el cliente no puede escribir a su derecha.
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat")
                .header("X-Forwarded-For", "9.9.9.9, 203.0.113.7"));

        assertThat(ip).isEqualTo("203.0.113.7");
    }

    @Test
    void variasIpsFalsasTampocoDesplazanALaReal() {
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat")
                .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2, 3.3.3.3, 203.0.113.7"));

        assertThat(ip).isEqualTo("203.0.113.7");
    }

    @Test
    void sinCabeceraDelClienteUsaLaIpQuePoneElProxy() {
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat")
                .header("X-Forwarded-For", "203.0.113.7"));

        assertThat(ip).isEqualTo("203.0.113.7");
    }

    @Test
    void laCabeceraEstandarForwardedSeIgnora() {
        // Railway no la emite, así que solo servía para suplantar.
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat")
                .header("Forwarded", "for=6.6.6.6"));

        assertThat(ip).isEqualTo(PROXY.getHostString());
    }

    @Test
    void sinCabecerasReenviadasNoToquetealaDireccion() {
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat"));

        assertThat(ip).isEqualTo(PROXY.getHostString());
    }

    @Test
    void toleraEspaciosYValoresVacios() {
        String ip = clientIpOf(MockServerHttpRequest.get("/api/chat")
                .header("X-Forwarded-For", "9.9.9.9 ,  203.0.113.7 , "));

        assertThat(ip).isEqualTo("203.0.113.7");
    }

    @Test
    void mantieneElProtocoloReenviadoParaQueSigaSaliendoHsts() {
        // Railway termina el TLS: sin X-Forwarded-Proto la app se creería HTTP.
        ServerHttpRequest request = transformer.apply(MockServerHttpRequest.get("http://adrian0511.dev/api/chat")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "9.9.9.9, 203.0.113.7")
                .remoteAddress(PROXY)
                .build());

        assertThat(request.getURI().getScheme()).isEqualTo("https");
        assertThat(request.getRemoteAddress().getHostString()).isEqualTo("203.0.113.7");
    }
}
