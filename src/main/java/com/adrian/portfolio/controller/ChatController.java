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

    // Topes de entrada: sin ellos, cualquiera puede inflar el coste por petición
    // mandando una pregunta enorme o un historial fabricado.
    //
    // El tope de la pregunta por sí solo no servía: el historial venía sin límite
    // por turno, así que el único techo real era el del cuerpo HTTP
    // (spring.codec.max-in-memory-size, 256 KB por defecto). Comprobado con curl:
    // un historial de 200 KB llegaba entero al prompt, ~500 veces lo que este tope
    // aparentaba permitir. Cada turno se recorta a lo que de verdad puede medir.
    private static final int MAX_QUESTION_LENGTH = 500;
    // Un turno del asistente lo generó el modelo con ai.max-tokens=600, así que no
    // da para mucho más; de sobra para no cortar una respuesta legítima.
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
