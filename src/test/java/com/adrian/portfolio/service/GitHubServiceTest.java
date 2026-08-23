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
                  {"name":"proyecto-valido","description":"Un proyecto","html_url":"https://github.com/adrian0511/proyecto-valido","language":"Java","fork":false,"topics":["spring","backend"],"pushed_at":"2026-08-20T18:49:41Z"}
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
        assertThat(dto.getPushed_at()).isEqualTo("2026-08-20T18:49:41Z");
        // "backend" es genérico y se descarta; "spring" sí describe el proyecto.
        assertThat(dto.getTopics()).containsExactly("spring");
    }

    @Test
    void priorizaTopicsConceptualesYDescartaGenericosYElDelLenguaje() {
        String json = """
                [
                  {"name":"cookbook","description":"d","html_url":"x","language":"Java","fork":false,
                   "topics":["clean-architecture","ddd","docker","full-stack","hexagonal-architecture","java","postgresql"]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        List<String> topics = service.getFeaturedRepo(5).block().get(0).getTopics();

        // clean-architecture y ddd caen por ser sinónimos de hexagonal-architecture,
        // así que los huecos restantes los ocupa el stack.
        assertThat(topics).containsExactly("hexagonal-architecture", "docker", "postgresql");
        assertThat(topics).doesNotContain("full-stack", "java");
    }

    @Test
    void noRepiteDosTopicsDelMismoGrupoDeSinonimos() {
        String json = """
                [
                  {"name":"finance","description":"d","html_url":"x","language":"Java","fork":false,
                   "topics":["artificial-intelligence","finance","generative-ai","jwt-authentication","security"]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        // De autenticación y de IA entra solo el de mayor prioridad de cada grupo:
        // "security" y "artificial-intelligence" quedan fuera por redundantes.
        assertThat(service.getFeaturedRepo(5).block().get(0).getTopics())
                .containsExactly("jwt-authentication", "generative-ai", "finance");
    }

    @Test
    void reservaHuecoParaElStackAunqueSobrenTopicsConceptuales() {
        String json = """
                [
                  {"name":"varios","description":"d","html_url":"x","language":"Java","fork":false,
                   "topics":["microservices","kafka","resilience4j","postgresql"]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        // 3 conceptuales disponibles, pero solo entran 2: el hueco que queda es
        // siempre para el stack.
        assertThat(service.getFeaturedRepo(5).block().get(0).getTopics())
                .containsExactly("microservices", "resilience4j", "postgresql");
    }

    @Test
    void repoSinTopicsDevuelveListaVaciaEnLugarDeNull() {
        String json = """
                [
                  {"name":"pelado","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        assertThat(service.getFeaturedRepo(5).block().get(0).getTopics()).isEmpty();
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
    void getAllRepos_devuelveTodosLosPublicosSinRecortarALosDestacados() {
        // El chat necesita la lista entera: con solo los destacados no podria
        // hablar de un proyecto que no cabe en la portada.
        String json = """
                [
                  {"name":"uno","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"dos","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"tres","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"cuatro","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"cinco","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"seis","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"siete","description":"d","html_url":"x","language":"Java","fork":false,"topics":[]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        assertThat(service.getAllRepos().block()).hasSize(7);
    }

    @Test
    void getAllRepos_aplicaLosMismosFiltrosQueLasTarjetas() {
        String json = """
                [
                  {"name":"adrian0511","description":"soy yo","html_url":"x","language":null,"fork":false,"topics":[]},
                  {"name":"forked-repo","description":"desc","html_url":"x","language":"Java","fork":true,"topics":[]},
                  {"name":"sin-desc","description":null,"html_url":"x","language":"Java","fork":false,"topics":[]},
                  {"name":"bueno","description":"Un proyecto","html_url":"x","language":"Java","fork":false,"topics":[]}
                ]
                """;

        GitHubService service = buildService(webClientReturning(json), "");

        assertThat(service.getAllRepos().block())
                .extracting(RepoDTO::getName)
                .containsExactly("bueno");
    }

    @Test
    void getAllRepos_cacheaYNoCompartePeticionConLasTarjetas() {
        AtomicInteger calls = new AtomicInteger();
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("[{\"name\":\"uno\",\"description\":\"d\",\"html_url\":\"x\",\"language\":\"Java\",\"fork\":false,\"topics\":[]}]")
                            .build());
                })
                .build();

        GitHubService service = buildService(client, "");
        service.getAllRepos().block();
        service.getAllRepos().block();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void getAllRepos_siGithubFallaDevuelveLaListaDeRespaldo() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("GitHub caído")))
                .build();

        assertThat(buildService(client, "").getAllRepos().block()).hasSize(5);
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
