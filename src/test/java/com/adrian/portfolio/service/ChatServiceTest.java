package com.adrian.portfolio.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.adrian.portfolio.dto.RepoDTO;

import io.github.adrian0511.prompt_link.dto.Message;
import io.github.adrian0511.prompt_link.exceptions.AiClientException;
import io.github.adrian0511.prompt_link.service.ReactiveAiService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ReactiveAiService aiService = mock(ReactiveAiService.class);
    private final GitHubService gitHubService = mock(GitHubService.class);
    private final ChatService chatService = new ChatService(aiService, gitHubService);

    private static final List<RepoDTO> REPOS = List.of(
            new RepoDTO("orderflow", "Plataforma de pedidos con microservicios",
                    "https://github.com/adrian0511/orderflow", "Java",
                    List.of("microservices", "kafka"), null),
            new RepoDTO("bug-hunt", "Rate limiter y acortador de URLs",
                    "https://github.com/adrian0511/bug-hunt", "Python", List.of(), null));

    @BeforeEach
    void stubRepos() {
        when(gitHubService.getAllRepos()).thenReturn(Mono.just(REPOS));
    }

    @SuppressWarnings("unchecked")
    private List<Message> capturedConversation() {
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).stream(captor.capture());
        return captor.getValue();
    }

    @Test
    void anteponeLasReglasYElPerfilComoMensajeDeSistema() {
        when(aiService.stream(anyList())).thenReturn(Flux.just("ok"));

        chatService.answer("¿Qué stack usa?", List.of()).blockLast();

        Message system = capturedConversation().get(0);
        assertThat(system.getRole()).isEqualTo("system");
        // El perfil viaja dentro del prompt de sistema: es la única fuente de datos.
        assertThat(system.getContent()).contains("Spring Boot", "adriangarces0310@gmail.com");
        assertThat(system.getContent()).contains("ÚNICAMENTE con datos que aparezcan en el PERFIL");
    }

    @Test
    void mantieneElHistorialEntreElSistemaYLaPreguntaNueva() {
        when(aiService.stream(anyList())).thenReturn(Flux.just("ok"));

        List<Message> history = List.of(Message.user("¿Sabe Java?"), Message.assistant("Sí."));
        chatService.answer("¿Y Kafka?", history).blockLast();

        List<Message> conversation = capturedConversation();
        assertThat(conversation).hasSize(4);
        assertThat(conversation.get(1).getContent()).isEqualTo("¿Sabe Java?");
        assertThat(conversation.get(2).getContent()).isEqualTo("Sí.");
        assertThat(conversation.get(3).getContent()).isEqualTo("¿Y Kafka?");
    }

    @Test
    void meteLosRepositoriosDeGithubEnElPromptDeSistema() {
        when(aiService.stream(anyList())).thenReturn(Flux.just("ok"));

        chatService.answer("¿Qué ha construido?", List.of()).blockLast();

        // El perfil solo detalla unos pocos; los demas llegan de GitHub, asi que
        // el chat puede hablar de un repo nuevo sin tocar profile.md.
        String system = capturedConversation().get(0).getContent();
        assertThat(system).contains("orderflow", "microservices", "[Java]");
        assertThat(system).contains("bug-hunt", "[Python]");
    }

    @Test
    void recortaLasDescripcionesLargasParaNoInflarElPrompt() {
        when(aiService.stream(anyList())).thenReturn(Flux.just("ok"));
        when(gitHubService.getAllRepos()).thenReturn(Mono.just(List.of(
                new RepoDTO("verboso", "x".repeat(400), "url", "Java", List.of(), null))));

        chatService.answer("hola", List.of()).blockLast();

        String system = capturedConversation().get(0).getContent();
        assertThat(system).contains("x".repeat(220) + "…");
        assertThat(system).doesNotContain("x".repeat(221));
    }

    @Test
    void propagaElErrorEnLugarDeTragarselo() {
        when(aiService.stream(anyList()))
                .thenReturn(Flux.error(new AiClientException("caído", 500, null)));

        // El servicio no traduce fallos: de eso se encarga ChatExceptionHandler.
        StepVerifier.create(chatService.answer("hola", List.of()))
                .expectError(AiClientException.class)
                .verify();
    }
}
