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
 * cookie tiene mensajes ilimitados. Las barreras reales son las de IP (una
 * dirección IPv4 o un /64 de IPv6 sí son un recurso escaso), por hora y por
 * día, y el tope diario global, que acota el gasto pase lo que pase.
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
    private final int maxPerIpPerDay;
    private final int maxPerDay;

    private final Map<String, Bucket> perIp = new ConcurrentHashMap<>();
    private final AtomicInteger usedToday = new AtomicInteger();
    private volatile LocalDate currentDay = LocalDate.now();

    public ChatRateLimitFilter(
            @Value("${chat.max-messages-per-session:20}") int maxPerSession,
            @Value("${chat.max-messages-per-ip-per-hour:15}") int maxPerIpPerHour,
            @Value("${chat.max-messages-per-ip-per-day:20}") int maxPerIpPerDay,
            @Value("${chat.max-messages-per-day:150}") int maxPerDay) {
        this.maxPerSession = maxPerSession;
        this.maxPerIpPerHour = maxPerIpPerHour;
        this.maxPerIpPerDay = maxPerIpPerDay;
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

    /**
     * Dos cupos sobre la misma red: el de la hora frena las rachas, y el del día
     * impide que una sola se lleve el presupuesto diario entero. Sin el segundo,
     * 15 mensajes/hora bastaban para que una máquina vaciara en una tarde la
     * cuota del día —hoy, 50 peticiones del tier gratuito de OpenRouter— y
     * dejara el chat mudo para cualquier visitante.
     */
    private boolean ipBudgetSpent(String ip) {
        // Un mapa sin tope sería su propio vector de abuso: muchas IPs falsas
        // podrían hincharlo hasta agotar la memoria. Podar tira de paso la cuenta
        // del día de esa red, pero para llegar aquí harían falta más redes
        // distintas de las que el tope diario global deja pasar.
        if (perIp.size() > MAX_TRACKED_IPS) {
            perIp.entrySet().removeIf(entry -> entry.getValue().idle());
        }

        Bucket bucket = perIp.computeIfAbsent(ip, key -> new Bucket());

        // En este orden: si ya se pasó de la hora, la petición no gasta cupo del
        // día. Se rechaza igual, así que contarla dos veces sería cobrarla dos veces.
        return bucket.hitsThisHour() > maxPerIpPerHour || bucket.hitsToday() > maxPerIpPerDay;
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    /** Lo que una red lleva gastado: en la hora en curso y en el día. */
    private static final class Bucket {
        private final AtomicInteger today = new AtomicInteger();
        private final AtomicInteger thisHour = new AtomicInteger();
        private Instant hourStart = Instant.now();

        // La ventana de la hora se reinicia sola; la del día la limpia el cambio
        // de fecha, que vacía el mapa entero.
        synchronized int hitsThisHour() {
            Instant now = Instant.now();
            if (now.isAfter(hourStart.plus(IP_WINDOW))) {
                hourStart = now;
                thisHour.set(0);
            }
            return thisHour.incrementAndGet();
        }

        int hitsToday() {
            return today.incrementAndGet();
        }

        synchronized boolean idle() {
            return Instant.now().isAfter(hourStart.plus(IP_WINDOW));
        }
    }
}
