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
 * Resuelve la IP del visitante a partir del <b>último</b> valor de
 * {@code X-Forwarded-For}, no del primero.
 *
 * <p><b>El problema.</b> Spring coge el valor más a la izquierda
 * ({@code ForwardedHeaderUtils.parseForwardedFor} → {@code getLeftMostValue}),
 * y un proxy <b>añade</b> la IP real por la derecha. Así que el valor izquierdo
 * es siempre el que escribió el propio cliente: bastaba mandar
 * {@code X-Forwarded-For: 9.9.9.9} —o la cabecera estándar {@code Forwarded}—
 * para estrenar un cupo limpio en cada petición y dejar inútil el límite por IP
 * de {@code ChatRateLimitFilter}. Verificado con curl: con la IP real añadida
 * detrás, {@code "7.7.7.7, 127.0.0.1"}, seguía ganando la falsa.
 *
 * <p>Esto no se puede arreglar en un {@code WebFilter}: el transformer borra las
 * cabeceras {@code X-Forwarded-*} del request antes de que corra ninguno, así
 * que para entonces solo queda el {@code remoteAddress} ya falseado.
 *
 * <p><b>El arreglo.</b> El último valor lo pone el proxy que tenemos delante
 * (Railway) a partir de la conexión real, y el cliente no puede escribir a su
 * derecha. Además se ignora la cabecera estándar {@code Forwarded}, que ningún
 * proxy nuestro emite y solo aportaba una segunda vía de suplantación.
 *
 * <p><b>Asume exactamente un proxy delante.</b> Si algún día se mete otra capa
 * (una CDN por encima de Railway), el último valor pasaría a ser la IP de esa
 * capa y todos los visitantes compartirían cupo. Molesto, pero falla <i>cerrado</i>:
 * de más restrictivo, nunca de más permisivo, que es como debe fallar un límite
 * de uso. Lo que no se puede es volver a confiar en el valor de la izquierda.
 *
 * <p>Para que esa suposición no se rompa en silencio, la primera vez que el
 * valor resuelto no parece una dirección pública se deja un aviso en el log:
 * ver {@link #esDeVisitante(String)}.
 *
 * <p>El nombre del bean es obligatorio: {@code WebHttpHandlerBuilder} busca el
 * transformer por el nombre {@code forwardedHeaderTransformer}, no por tipo.
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
     * Si el último valor no es una dirección pública, es que delante hay más de
     * un salto y lo que estamos tomando por el visitante es un proxy interno:
     * todos compartirían cupo en {@code ChatRateLimitFilter}. Falla cerrado, así
     * que no rompe nada, pero dejaría el chat en 15 mensajes/hora para todo el
     * mundo sin que nadie se entere. De ahí el aviso.
     *
     * <p>Una sola vez por arranque: es una condición de configuración, no un
     * evento por petición, y repetirlo llenaría el log del incidente.
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
