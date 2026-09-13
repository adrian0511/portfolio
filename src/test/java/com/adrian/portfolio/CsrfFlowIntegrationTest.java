package com.adrian.portfolio;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.adrian.portfolio.dto.RepoDTO;
import com.adrian.portfolio.service.GitHubService;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Verifica el flujo csrf-token -> projects de punta a punta, tal como lo consume
// el frontend: cookie XSRF-TOKEN + la misma cadena en la cabecera X-XSRF-TOKEN.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CsrfFlowIntegrationTest {

    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private GitHubService gitHubService;

    private List<String> setCookies() {
        FluxExchangeResult<Void> result = client.get().uri("/api/csrf-token")
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(Void.class);

        List<String> cookies = result.getResponseHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        return cookies;
    }

    private String token() {
        return setCookies().stream()
                .filter(cookie -> cookie.startsWith(COOKIE + "="))
                .map(cookie -> cookie.split(";")[0].split("=", 2)[1])
                .findFirst()
                .orElseThrow(() -> new AssertionError("no se emitió la cookie " + COOKIE));
    }

    /**
     * La regresión que importa: pedir el token creaba una WebSession, y como el
     * endpoint es público y sin cupo, unos miles de peticiones llenaban el
     * almacén en memoria (tope 10.000) y la web pasaba a responder 500 a todos.
     */
    @Test
    void pedirElTokenNoCreaSesionEnServidor() {
        assertThat(setCookies()).noneMatch(cookie -> cookie.startsWith("SESSION="));
    }

    @Test
    void tokenValidoConSuMismaCookiePermiteConsultarProyectos() {
        when(gitHubService.getFeaturedRepo(5))
                .thenReturn(Mono.just(List.of(
                        new RepoDTO("demo", "desc", "url", "Java", List.of("spring-boot"), "2026-08-20T18:49:41Z"))));

        String token = token();

        client.get().uri("/api/projects")
                .cookie(COOKIE, token)
                .header(HEADER, token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepoDTO.class)
                .hasSize(1);
    }

    @Test
    void sinCabeceraDevuelve404AunqueLaCookieSeaValida() {
        client.get().uri("/api/projects")
                .cookie(COOKIE, token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void tokenValidoSinSuCookieDevuelve404() {
        client.get().uri("/api/projects")
                .header(HEADER, token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void cabeceraQueNoCoincideConLaCookieDevuelve404() {
        client.get().uri("/api/projects")
                .cookie(COOKIE, token())
                .header(HEADER, "token-de-otro")
                .exchange()
                .expectStatus().isNotFound();
    }
}
