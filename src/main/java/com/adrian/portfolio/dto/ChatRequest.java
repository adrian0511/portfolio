package com.adrian.portfolio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    private String question;

    /** Turnos previos, para que el chat mantenga el hilo. */
    private List<ChatTurn> history;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatTurn {
        /** "user" o "assistant"; cualquier otro valor se descarta. */
        private String role;
        private String content;
    }
}
