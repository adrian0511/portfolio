package com.adrian.portfolio.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.CookieWebSessionIdResolver;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // 'unsafe-inline' en style-src es necesario porque React aplica estilos como
    // atributo style="..."; sin él la página se renderiza sin esos estilos.
    // Los scripts, en cambio, van todos en el bundle: script-src queda en 'self'.
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "font-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "object-src 'none'");

    /**
     * La cookie de sesión salía solo con HttpOnly. SameSite=Lax impide que otro
     * sitio la use en peticiones cross-site, y Secure evita que viaje en claro;
     * este último se activa por variable de entorno porque en local se sirve por
     * HTTP y el navegador descartaría una cookie marcada como Secure.
     */
    @Bean
    WebSessionIdResolver webSessionIdResolver(
            @Value("${session.cookie.secure:false}") boolean secure) {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.addCookieInitializer(cookie -> cookie
                .httpOnly(true)
                .sameSite("Lax")
                .secure(secure)
                .path("/"));
        return resolver;
    }

    /**
     * Token en cookie y no en sesión (el repositorio por defecto) porque quien lo
     * tiene que leer es el JavaScript del navegador: de ahí withHttpOnlyFalse().
     * Secure va condicionado por la misma razón que la cookie de sesión.
     */
    @Bean
    ServerCsrfTokenRepository csrfTokenRepository(
            @Value("${session.cookie.secure:false}") boolean secure) {
        CookieServerCsrfTokenRepository repository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .sameSite("Lax")
                .secure(secure)
                .path("/"));
        return repository;
    }

    /**
     * El CSRF nativo cubre POST /api/chat, que es lo que le corresponde: una
     * petición con efecto (gasta cuota del modelo) y con el método que el
     * mecanismo estándar protege. El filtro propio se queda solo con
     * GET /api/projects, que el nativo no puede proteger por diseño.
     *
     * @see com.adrian.portfolio.security.filter.CsrfValidationFilter
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ServerCsrfTokenRepository csrfTokenRepository) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // El handler por defecto (XOR, protección BREACH) enmascara el
                        // token por petición y espera recibirlo enmascarado; el cliente
                        // devuelve el valor tal cual lo lee de la cookie, así que hay que
                        // usar el plano. BREACH no aplica aquí: el token no se incrusta
                        // en el HTML comprimido, viaja en una cabecera Set-Cookie.
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler()))
                .headers(headers -> headers
                        // Su política por defecto (no-store en todo) impedía cachear
                        // los estáticos; CacheControlFilter la sustituye por ruta.
                        .cache(cache -> cache.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP)))
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll())
                .build();
    }

}
