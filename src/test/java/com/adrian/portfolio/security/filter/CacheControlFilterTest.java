package com.adrian.portfolio.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CacheControlFilterTest {

    private final CacheControlFilter filter = new CacheControlFilter();

    private final WebFilterChain chain = exchange -> {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return exchange.getResponse().setComplete();
    };

    private String cacheControlFor(String path) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        filter.filter(exchange, chain).block();
        return exchange.getResponse().getHeaders().getCacheControl();
    }

    @Test
    void losAssetsConHashSeCacheanComoInmutables() {
        assertThat(cacheControlFor("/assets/index-CmHClXJm.js"))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(cacheControlFor("/fonts/JetBrainsMono-Regular.woff2"))
                .isEqualTo("public, max-age=31536000, immutable");
    }

    @Test
    void losFicherosDeNombreEstableSeCacheanUnDia() {
        assertThat(cacheControlFor("/img/Avatar.webp")).isEqualTo("public, max-age=86400");
        assertThat(cacheControlFor("/docs/CV_Adrian_Garces_ES.pdf")).isEqualTo("public, max-age=86400");
        assertThat(cacheControlFor("/favicon.svg")).isEqualTo("public, max-age=86400");
    }

    @Test
    void indexHtmlNuncaSeCachea() {
        // Si se cacheara, un despliegue nuevo no llegaría al navegador porque el
        // HTML es quien apunta a los assets con hash.
        assertThat(cacheControlFor("/")).isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
        assertThat(cacheControlFor("/index.html")).isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
    }

    @Test
    void lasRespuestasDeLaApiNuncaSeCachean() {
        assertThat(cacheControlFor("/api/projects")).isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
        assertThat(cacheControlFor("/api/csrf-token")).isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
    }
}
