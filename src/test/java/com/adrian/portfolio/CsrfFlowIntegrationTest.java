package com.adrian.portfolio;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.adrian.portfolio.dto.RepoDTO;
import com.adrian.portfolio.service.GitHubService;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Verifica el flujo CSRF (csrf-token -> projects) de punta a punta, tal como
// lo consume el frontend: cookie de sesión + header X-CSRF-Token coincidiendo
// con el token guardado en esa sesión.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CsrfFlowIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, String>> TOKEN_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private GitHubService gitHubService;

    @Test
    void tokenValidoConSuMismaSesionPermiteConsultarProyectos() {
        when(gitHubService.getFeaturedRepo(5))
                .thenReturn(Mono.just(List.of(new RepoDTO("demo", "desc", "url", "Java", "backend"))));

        EntityExchangeResult<Map<String, String>> tokenResult = client.get().uri("/api/csrf-token")
                .exchange().expectStatus().isOk().expectBody(TOKEN_TYPE).returnResult();

        String token = tokenResult.getResponseBody().get("token");
        String[] cookiePair = cookiePair(tokenResult);

        client.get().uri("/api/projects")
                .cookie(cookiePair[0], cookiePair[1])
                .header("X-CSRF-Token", token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepoDTO.class)
                .hasSize(1);
    }

    @Test
    void sinHeaderCsrfDevuelve404AunqueLaCookieDeSesionSeaValida() {
        EntityExchangeResult<Map<String, String>> tokenResult = client.get().uri("/api/csrf-token")
                .exchange().expectStatus().isOk().expectBody(TOKEN_TYPE).returnResult();
        String[] cookiePair = cookiePair(tokenResult);

        client.get().uri("/api/projects")
                .cookie(cookiePair[0], cookiePair[1])
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void tokenValidoSinLaCookieDeSuSesionDevuelve404() {
        EntityExchangeResult<Map<String, String>> tokenResult = client.get().uri("/api/csrf-token")
                .exchange().expectStatus().isOk().expectBody(TOKEN_TYPE).returnResult();
        String token = tokenResult.getResponseBody().get("token");

        // Sin la cookie de esa sesión, el backend ve una sesión nueva sin ese token.
        client.get().uri("/api/projects")
                .header("X-CSRF-Token", token)
                .exchange()
                .expectStatus().isNotFound();
    }

    private String[] cookiePair(EntityExchangeResult<Map<String, String>> result) {
        String setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        return setCookie.split(";")[0].split("=", 2);
    }
}
