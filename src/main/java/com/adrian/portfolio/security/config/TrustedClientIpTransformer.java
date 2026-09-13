package com.adrian.portfolio.security.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import lombok.extern.log4j.Log4j2;

/**
 * Resuelve la IP del visitante desde el <b>último</b> valor de
 * {@code X-Forwarded-For}, no desde el primero.
 *
 * <p>Spring usa el valor más a la izquierda, que es siempre el que escribió el
 * cliente: un proxy añade la IP real por la derecha. Mandando
 * {@code X-Forwarded-For: 9.9.9.9} —o la cabecera {@code Forwarded}, que por eso
 * se ignora— se estrenaba cupo en cada petición y el límite por IP de
 * {@code ChatRateLimitFilter} no protegía nada.
 *
 * <p>No sirve hacerlo en un {@code WebFilter}: el transformer borra las
 * cabeceras {@code X-Forwarded-*} antes de que corra ninguno.
 *
 * <p><b>Asume exactamente un proxy delante.</b> Con otra capa (una CDN sobre
 * Railway) el último valor sería el de esa capa y todos los visitantes
 * compartirían cupo: falla cerrado, y {@link #esDeVisitante(String)} lo avisa en
 * el log. Lo que no se puede es volver a confiar en el valor de la izquierda.
 *
 * <p>El nombre del bean es obligatorio: {@code WebHttpHandlerBuilder} busca el
 * transformer por nombre, no por tipo.
 */
@Component("forwardedHeaderTransformer")
@Log4j2
public class TrustedClientIpTransformer extends ForwardedHeaderTransformer {

    private static final String HEADER = "X-Forwarded-For";

    // 100.64.0.0/10 (CGNAT): un proxy intermedio puede usarla, un visitante no.
    private static final int CGNAT_FIRST_BYTE = 100;
    private static final int CGNAT_SECOND_BYTE_MIN = 64;
    private static final int CGNAT_SECOND_BYTE_MAX = 127;
    // fc00::/7: el equivalente IPv6, que isSiteLocalAddress() no cubre.
    private static final int IPV6_UNIQUE_LOCAL_MASK = 0xFE;
    private static final int IPV6_UNIQUE_LOCAL_PREFIX = 0xFC;

    private final AtomicBoolean avisado = new AtomicBoolean();

    public TrustedClientIpTransformer() {
        // false = usar solo las cabeceras X-Forwarded-*, ignorando "Forwarded".
        super(false);
    }

    @Override
    public ServerHttpRequest apply(ServerHttpRequest request) {
        // Hay que leerla antes: super.apply() borra las cabeceras X-Forwarded-*.
        String forwardedFor = request.getHeaders().getFirst(HEADER);

        ServerHttpRequest applied = super.apply(request);
        if (forwardedFor == null) {
            return applied;
        }

        String client = rightMostValue(forwardedFor);
        if (client == null) {
            return applied;
        }

        avisarSiNoPareceVisitante(client, forwardedFor);

        InetSocketAddress spoofable = applied.getRemoteAddress();
        int port = spoofable == null ? 0 : spoofable.getPort();

        return applied.mutate()
                .remoteAddress(InetSocketAddress.createUnresolved(bracketIpv6(client), port))
                .build();
    }

    /**
     * Una dirección privada como último valor significa que delante hay más de un
     * salto: el chat quedaría en un solo cupo para todos, en silencio. Se avisa
     * una vez por arranque, que es una condición de configuración y no un evento
     * por petición.
     */
    private void avisarSiNoPareceVisitante(String client, String header) {
        if (esDeVisitante(client) || !avisado.compareAndSet(false, true)) {
            return;
        }

        log.warn("La IP que se toma por la del visitante ({}) no es publica, asi que delante hay mas "
                + "de un proxy y todos los visitantes comparten el cupo del chat. X-Forwarded-For: '{}'",
                client, header);
    }

    /** Sin DNS: {@code ofLiteral} rechaza lo que no sea una IP en vez de resolverlo. */
    static boolean esDeVisitante(String value) {
        InetAddress address;
        try {
            address = InetAddress.ofLiteral(value);
        } catch (IllegalArgumentException noEsUnaIp) {
            return false;
        }

        if (address.isLoopbackAddress() || address.isAnyLocalAddress()
                || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return (bytes[0] & IPV6_UNIQUE_LOCAL_MASK) != IPV6_UNIQUE_LOCAL_PREFIX;
        }

        int second = bytes[1] & 0xFF;
        return !((bytes[0] & 0xFF) == CGNAT_FIRST_BYTE
                && second >= CGNAT_SECOND_BYTE_MIN && second <= CGNAT_SECOND_BYTE_MAX);
    }

    private String rightMostValue(String header) {
        String[] values = header.split(",");
        for (int i = values.length - 1; i >= 0; i--) {
            String value = values[i].trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    // Mismo tratamiento que hace Spring con el valor que sí usa.
    private String bracketIpv6(String host) {
        boolean ipv6 = host.indexOf(':') != -1;
        return ipv6 && !host.startsWith("[") && !host.endsWith("]") ? "[" + host + "]" : host;
    }
}
