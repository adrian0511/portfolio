package com.adrian.portfolio.security.config;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "203.0.113.7", "2001:db8::1", "8.8.8.8", "::ffff:203.0.113.7"
    })
    void reconoceComoVisitanteUnaDireccionPublica(String ip) {
        assertThat(TrustedClientIpTransformer.esDeVisitante(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",      // loopback: el propio contenedor
            "10.0.0.1",       // RFC1918: otro salto interno
            "192.168.1.1",
            "172.16.0.1",
            "100.64.0.1",     // CGNAT: la usan los proxies, no los visitantes
            "169.254.0.1",    // link-local
            "::1",
            "fd00::1",        // unique local IPv6, que isSiteLocalAddress() no ve
            "0.0.0.0",
            "desconocida",    // ni siquiera es una IP
    })
    void avisaDeQueNoEsLaDireccionDeUnVisitante(String ip) {
        // Si el ultimo valor de X-Forwarded-For es esto, delante hay mas de un
        // proxy y el cupo por IP del chat pasa a ser uno solo para todo el mundo.
        assertThat(TrustedClientIpTransformer.esDeVisitante(ip)).isFalse();
    }

    @Test
    void comprobarLaDireccionNoResuelvePorDns() {
        // El valor viene de una cabecera: resolverlo seria una peticion saliente
        // por cada visita, dirigida por quien manda la cabecera.
        assertThat(TrustedClientIpTransformer.esDeVisitante("localhost")).isFalse();
        assertThat(TrustedClientIpTransformer.esDeVisitante("adrian0511.dev")).isFalse();
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
