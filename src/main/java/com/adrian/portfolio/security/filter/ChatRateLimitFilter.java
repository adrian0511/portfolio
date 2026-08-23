package com.adrian.portfolio.security.filter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

/**
 * Cupos del chat. El de sesión por sí solo no protege nada: crear una sesión
 * cuesta una petición a /api/csrf-token, así que un script que descarte la
 * cookie tiene mensajes ilimitados. Las barreras reales son la de IP (las
 * direcciones sí son un recurso escaso) y el tope diario global, que acota el
 * gasto pase lo que pase.
 *
 * No se filtra por Origin ni Referer: el navegador no manda Origin en este POST
 * y la cabecera Referrer-Policy: no-referrer impide el Referer, así que exigir
 * cualquiera de las dos bloquearía a los visitantes de verdad.
 */
@Component
@Order(-95)
@Log4j2
public class ChatRateLimitFilter implements WebFilter {

    private static final String CHAT_PATH = "/api/chat";
    private static final String COUNT_ATTR = "CHAT_COUNT";
    private static final Duration IP_WINDOW = Duration.ofHours(1);
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int maxPerSession;
    private final int maxPerIpPerHour;
    private final int maxPerDay;

    private final Map<String, Window> perIp = new ConcurrentHashMap<>();
    private final AtomicInteger usedToday = new AtomicInteger();
    private volatile LocalDate currentDay = LocalDate.now();

    public ChatRateLimitFilter(
            @Value("${chat.max-messages-per-session:20}") int maxPerSession,
            @Value("${chat.max-messages-per-ip-per-hour:15}") int maxPerIpPerHour,
            @Value("${chat.max-messages-per-day:150}") int maxPerDay) {
        this.maxPerSession = maxPerSession;
        this.maxPerIpPerHour = maxPerIpPerHour;
        this.maxPerDay = maxPerDay;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!CHAT_PATH.equals(exchange.getRequest().getURI().getPath())) {
            return chain.filter(exchange);
        }

        if (dailyBudgetSpent()) {
            log.warn("Cupo diario del chat agotado ({} mensajes)", maxPerDay);
            return reject(exchange);
        }

        if (ipBudgetSpent(clientIp(exchange))) {
            return reject(exchange);
        }

        return exchange.getSession().flatMap(session -> {
            int used = session.getAttributeOrDefault(COUNT_ATTR, 0);
            if (used >= maxPerSession) {
                return reject(exchange);
            }

            session.getAttributes().put(COUNT_ATTR, used + 1);
            usedToday.incrementAndGet();
            return chain.filter(exchange);
        });
    }

    /**
     * Con server.forward-headers-strategy=framework, Spring ya resuelve la IP
     * real del visitante a partir de X-Forwarded-For que añade el proxy de
     * Railway; sin eso, todas las peticiones parecerían venir del proxy.
     */
    private String clientIp(ServerWebExchange exchange) {
        var address = exchange.getRequest().getRemoteAddress();
        return address == null ? "desconocida" : address.getAddress().getHostAddress();
    }

    private boolean dailyBudgetSpent() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            synchronized (this) {
                if (!today.equals(currentDay)) {
                    currentDay = today;
                    usedToday.set(0);
                    perIp.clear();
                }
            }
        }
        return usedToday.get() >= maxPerDay;
    }

    private boolean ipBudgetSpent(String ip) {
        // Un mapa sin tope sería su propio vector de abuso: muchas IPs falsas
        // podrían hincharlo hasta agotar la memoria.
        if (perIp.size() > MAX_TRACKED_IPS) {
            perIp.entrySet().removeIf(entry -> entry.getValue().expired());
        }

        Window window = perIp.compute(ip,
                (key, current) -> current == null || current.expired() ? new Window() : current);

        return window.hits.incrementAndGet() > maxPerIpPerHour;
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private static final class Window {
        private final Instant start = Instant.now();
        private final AtomicInteger hits = new AtomicInteger();

        boolean expired() {
            return Instant.now().isAfter(start.plus(IP_WINDOW));
        }
    }
}
