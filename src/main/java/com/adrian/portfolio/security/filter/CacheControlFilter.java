package com.adrian.portfolio.security.filter;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Spring Security marca por defecto toda respuesta como {@code no-store}, lo que
 * impide cachear también los estáticos. Este filtro afina esa política por ruta.
 */
@Component
@Order(-90)
public class CacheControlFilter implements WebFilter {

    // Vite genera estos ficheros con un hash del contenido en el nombre: si el
    // contenido cambia, cambia la URL, así que cachearlos un año es seguro.
    // Ojo: sustituir un fichero de /fonts/ obliga a renombrarlo.
    private static final List<String> IMMUTABLE_PATHS = List.of("/assets/", "/fonts/");
    private static final String IMMUTABLE = "public, max-age=31536000, immutable";

    // Nombres estables que sí pueden cambiar (avatar, CV): un día de caché
    // acelera las visitas repetidas sin dejar el contenido viejo mucho tiempo.
    private static final List<String> STABLE_NAME_PATHS = List.of("/img/", "/docs/", "/favicon.svg");
    private static final String ONE_DAY = "public, max-age=86400";

    // index.html referencia los assets con hash, así que nunca debe cachearse:
    // si se cacheara, un despliegue nuevo no llegaría al navegador.
    private static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String cacheControl = cacheControlFor(path);

        // beforeCommit para que el valor gane al del manejador de recursos estáticos.
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().setCacheControl(cacheControl);
            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private String cacheControlFor(String path) {
        if (IMMUTABLE_PATHS.stream().anyMatch(path::startsWith)) {
            return IMMUTABLE;
        }
        if (STABLE_NAME_PATHS.stream().anyMatch(path::startsWith)) {
            return ONE_DAY;
        }
        return NO_STORE;
    }
}
