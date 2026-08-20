package com.adrian.portfolio.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.adrian.portfolio.dto.RepoDTO;
import com.adrian.portfolio.service.GitHubService;

import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectControllerTest {

    private final GitHubService service = mock(GitHubService.class);
    private final WebTestClient client = WebTestClient.bindToController(new ProjectController(service)).build();

    @Test
    void devuelve200ConLaListaDeProyectosCuandoHayResultados() {
        RepoDTO repo = new RepoDTO("demo", "desc", "https://github.com/adrian0511/demo", "Java", "backend");
        when(service.getFeaturedRepo(5)).thenReturn(Mono.just(List.of(repo)));

        client.get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepoDTO.class)
                .hasSize(1);
    }

    @Test
    void devuelve200ConListaVaciaCuandoElServicioNoEncuentraRepos() {
        // GitHubService siempre emite una lista (vacía como mucho), nunca un Mono vacío,
        // así que este es el camino real cuando no hay proyectos que mostrar.
        when(service.getFeaturedRepo(5)).thenReturn(Mono.just(List.of()));

        client.get().uri("/api/projects")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepoDTO.class)
                .hasSize(0);
    }

    @Test
    void devuelve204SiElServicioNoEmiteNingunValor() {
        // Camino defensivo de defaultIfEmpty(): no se alcanza con la implementación
        // actual de GitHubService, pero cubre el contrato del controller.
        when(service.getFeaturedRepo(5)).thenReturn(Mono.empty());

        client.get().uri("/api/projects")
                .exchange()
                .expectStatus().isNoContent();
    }
}
