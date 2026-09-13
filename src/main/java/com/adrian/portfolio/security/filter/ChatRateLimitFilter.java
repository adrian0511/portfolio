package com.adrian.portfolio.security.filter;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HexFormat;
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
 * cookie tiene mensajes ilimitados. Las barreras reales son la de IP (una
 * dirección IPv4 o un /64 de IPv6 sí son un recurso escaso) y el tope diario
 * global, que acota el gasto pase lo que pase.
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
    // Los 8 primeros bytes de una IPv6: su /64.
    private static final int IPV6_PREFIX_BYTES = 8;

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
        // getHostString() y no getAddress().getHostAddress(): detrás del proxy la
        // dirección viene SIN resolver (createUnresolved a partir del
        // X-Forwarded-For) y getAddress() devuelve null.
        return address == null ? "desconocida" : bucketOf(address.getHostString());
    }

    /**
     * En IPv4 el cupo va por dirección; en IPv6, por /64.
     *
     * <p>Una dirección IPv6 suelta no es un recurso escaso: al visitante
     * doméstico se le asigna un /64 entero, así que puede estrenar dirección en
     * cada petición sin coste. Comprobado contra el jar: 20 peticiones rotando
     * {@code 2001:db8:1:1::N} pasaban enteras con el cupo por hora en 15, y solo
     * las frenaba el tope diario global. El /64 es el bloque más pequeño que se
     * reparte de una pieza, así que es la unidad que de verdad cuesta conseguir.
     */
    private String bucketOf(String host) {
        try {
            // ofLiteral y no getByName: este valor viene de una cabecera, y
            // getByName resolvería por DNS lo que no sea una IP.
            if (InetAddress.ofLiteral(host) instanceof Inet6Address ipv6) {
                byte[] prefix = Arrays.copyOf(ipv6.getAddress(), IPV6_PREFIX_BYTES);
                return HexFormat.of().formatHex(prefix) + "::/64";
            }
        } catch (IllegalArgumentException noEsUnaIp) {
            // Un valor que no es una IP literal se agrupa tal cual: no se pierde
            // el cupo, solo deja de haber agrupación por prefijo.
        }
        return host;
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
