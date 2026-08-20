package com.adrian.portfolio.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.adrian.portfolio.dto.RepoDTO;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubServiceTest {

    private GitHubService buildService(WebClient webClient, String token) {
        GitHubService service = new GitHubService();
        ReflectionTestUtils.setField(service, "webClient", webClient);
        ReflectionTestUtils.setField(service, "username", "adrian0511");
        ReflectionTestUtils.setField(service, "token", token);
        ReflectionTestUtils.setField(service, "cacheTtlSeconds", 600L);
        return service;
    }

    private WebClient webClientReturning(String jsonBody) {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(jsonBody)
                        .build()))
                .build();
    }

    @Test
    void filtraForksRepoHomonimoYSinDescripcion_yMapeaCamposCorrectamente() {
        String json = """
                [
                  {"name":"adrian0511","description":"soy yo","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"forked-repo","description":"desc","html_url":"x","language":"Java","fork":true,"topics":[]},
                  {"name":"sin-desc","description":null,"html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"proyecto-valido","description":"Un proyecto","html_url":"https://github.com/adrian0511/proyecto-valido","language":"Java","fork":false,"topics":["spring","backend"]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        List<RepoDTO> result = service.getFeaturedRepo(5).block();

        assertThat(result).hasSize(1);
        RepoDTO dto = result.get(0);
        assertThat(dto.getName()).isEqualTo("proyecto-valido");
        assertThat(dto.getDescription()).isEqualTo("Un proyecto");
        assertThat(dto.getHtml_url()).isEqualTo("https://github.com/adrian0511/proyecto-valido");
        assertThat(dto.getLanguage()).isEqualTo("Java");
        assertThat(dto.getTopic()).isIn("spring", "backend");
    }

    @Test
    void respetaElLimiteDeRepos() {
        String json = """
                [
                  {"name":"r1","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"r2","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"r3","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        List<RepoDTO> result = service.getFeaturedRepo(2).block();

        assertThat(result).hasSize(2);
    }

    @Test
    void siGithubFalla_devuelveListaDeRespaldoDe5Repos() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("GitHub caído")))
                .build();

        GitHubService service = buildService(client, "");

        List<RepoDTO> result = service.getFeaturedRepo(5).block();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(RepoDTO::getName)
                .contains("distributed-ecommerce-platform", "challengehub", "biblioteca",
                        "Gestion-de-Reservas", "RetosConIA");
    }

    @Test
    void cacheaLaRespuestaYNoRepiteLaLlamadaHttp() {
        AtomicInteger calls = new AtomicInteger();
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("[]")
                            .build());
                })
                .build();

        GitHubService service = buildService(client, "");

        service.getFeaturedRepo(5).block();
        service.getFeaturedRepo(5).block();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void incluyeAuthorizationBearerSoloSiHayTokenConfigurado() {
        AtomicReference<String> authHeader = new AtomicReference<>();
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    authHeader.set(request.headers().getFirst("Authorization"));
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("[]")
                            .build());
                })
                .build();

        buildService(client, "mi-token").getFeaturedRepo(5).block();
        assertThat(authHeader.get()).isEqualTo("Bearer mi-token");

        buildService(client, "").getFeaturedRepo(5).block();
        assertThat(authHeader.get()).isNull();
    }
}
