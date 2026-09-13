package com.adrian.portfolio.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adrian.portfolio.dto.ChatRequest;
import com.adrian.portfolio.service.ChatService;

import io.github.adrian0511.prompt_link.dto.Message;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    // Sin un tope POR TURNO estos otros dos no valen nada: el techo real pasa a ser
    // el del cuerpo HTTP (spring.codec.max-in-memory-size, 256 KB) y un historial
    // de 200 KB llega entero al prompt.
    private static final int MAX_QUESTION_LENGTH = 500;
    // Lo generó el modelo con ai.max-tokens=600, así que no da para más.
    private static final int MAX_ANSWER_LENGTH = 2000;
    private static final int MAX_HISTORY_TURNS = 6;

    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isEmpty()) {
            return Flux.just("Escribe una pregunta sobre el perfil de Adrián.");
        }

        return chatService.answer(
                clip(question, MAX_QUESTION_LENGTH),
                toMessages(request.getHistory()));
    }

    private List<Message> toMessages(List<ChatRequest.ChatTurn> history) {
        if (history == null) {
            return List.of();
        }

        List<ChatRequest.ChatTurn> recent = history.size() > MAX_HISTORY_TURNS
                ? history.subList(history.size() - MAX_HISTORY_TURNS, history.size())
                : history;

        return recent.stream()
                .filter(turn -> turn.getContent() != null && !turn.getContent().isBlank())
                // Solo user/assistant: aceptar "system" dejaría que el cliente
                // reescribiese las reglas del asistente desde el navegador.
                .map(turn -> "assistant".equals(turn.getRole())
                        ? Message.assistant(clip(turn.getContent(), MAX_ANSWER_LENGTH))
                        : Message.user(clip(turn.getContent(), MAX_QUESTION_LENGTH)))
                .toList();
    }

    private String clip(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit) : text;
    }
}
