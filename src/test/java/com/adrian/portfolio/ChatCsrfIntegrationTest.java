package com.adrian.portfolio;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.adrian.portfolio.service.ChatService;
import com.adrian.portfolio.service.GitHubService;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El chat va por el CSRF nativo de Spring Security (cookie XSRF-TOKEN + cabecera
 * X-XSRF-TOKEN), no por CsrfValidationFilter. Al vivir dentro de la cadena de
 * Spring Security, solo se puede comprobar levantando el contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ChatCsrfIntegrationTest {

    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private GitHubService gitHubService;

    private String csrfCookie() {
        List<String> cookies = client.get().uri("/api/csrf-token")
                .exchange().expectStatus().isNoContent()
                .returnResult(Void.class)
                .getResponseHeaders().get(HttpHeaders.SET_COOKIE);

        assertThat(cookies).isNotNull();
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(COOKIE + "="))
                .map(cookie -> cookie.split(";")[0].split("=", 2)[1])
                .findFirst()
                .orElseThrow(() -> new AssertionError("no se emitió la cookie " + COOKIE));
    }

    @Test
    void unaRespuestaDeApiEmiteLaCookieConElToken() {
        assertThat(csrfCookie()).isNotBlank();
    }

    @Test
    void elTokenNoViajaConLosEstaticos() {
        // Emitirlo junto a un asset cacheado como immutable dejaría que una caché
        // compartida sirviera el mismo token a todos los visitantes.
        List<String> cookies = client.get().uri("/assets/no-existe.js")
                .exchange()
                .returnResult(Void.class)
                .getResponseHeaders().get(HttpHeaders.SET_COOKIE);

        assertThat(cookies == null ? List.<String>of() : cookies)
                .noneMatch(cookie -> cookie.startsWith(COOKIE + "="));
    }

    @Test
    void conCookieYCabeceraCoincidentesLlegaAlChat() {
        when(chatService.answer(anyString(), any())).thenReturn(Flux.just("Usa Java."));
        String token = csrfCookie();

        client.post().uri("/api/chat")
                .cookie(COOKIE, token)
                .header(HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"¿Qué usa?\",\"history\":[]}")
                .exchange()
                .expectStatus().isOk();

        verify(chatService).answer(anyString(), any());
    }

    @Test
    void sinCabeceraCsrfDevuelve403YNoLlamaAlModelo() {
        String token = csrfCookie();

        client.post().uri("/api/chat")
                .cookie(COOKIE, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"¿Qué usa?\",\"history\":[]}")
                .exchange()
                .expectStatus().isForbidden();

        verify(chatService, never()).answer(anyString(), any());
    }

    @Test
    void conCabeceraQueNoCoincideConLaCookieDevuelve403() {
        String token = csrfCookie();

        client.post().uri("/api/chat")
                .cookie(COOKIE, token)
                .header(HEADER, "token-de-otro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"¿Qué usa?\",\"history\":[]}")
                .exchange()
                .expectStatus().isForbidden();

        verify(chatService, never()).answer(anyString(), any());
    }
}
