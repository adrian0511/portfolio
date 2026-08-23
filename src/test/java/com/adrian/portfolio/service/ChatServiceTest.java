package com.adrian.portfolio.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.adrian0511.prompt_link.dto.Message;
import io.github.adrian0511.prompt_link.exceptions.AiClientException;
import io.github.adrian0511.prompt_link.service.ReactiveAiService;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ReactiveAiService aiService = mock(ReactiveAiService.class);
    private final ChatService chatService = new ChatService(aiService);

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
    void propagaElErrorEnLugarDeTragarselo() {
        when(aiService.stream(anyList()))
                .thenReturn(Flux.error(new AiClientException("caído", 500, null)));

        // El servicio no traduce fallos: de eso se encarga ChatExceptionHandler.
        StepVerifier.create(chatService.answer("hola", List.of()))
                .expectError(AiClientException.class)
                .verify();
    }
}
