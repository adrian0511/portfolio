package com.adrian.portfolio.security.config;

import java.net.InetSocketAddress;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

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
 * <p>El nombre del bean es obligatorio: {@code WebHttpHandlerBuilder} busca el
 * transformer por el nombre {@code forwardedHeaderTransformer}, no por tipo.
 */
@Component("forwardedHeaderTransformer")
public class TrustedClientIpTransformer extends ForwardedHeaderTransformer {

    private static final String HEADER = "X-Forwarded-For";

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

        InetSocketAddress spoofable = applied.getRemoteAddress();
        int port = spoofable == null ? 0 : spoofable.getPort();

        return applied.mutate()
                .remoteAddress(InetSocketAddress.createUnresolved(bracketIpv6(client), port))
                .build();
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
