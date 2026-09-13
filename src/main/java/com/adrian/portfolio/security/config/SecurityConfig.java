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
     * {@code Secure} va por variable de entorno y no fijo: en local se sirve por
     * HTTP y el navegador descartaría la cookie.
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

    /** En cookie y no en sesión: quien lee el token es el JavaScript del navegador. */
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
     * El CSRF nativo cubre {@code POST /api/chat}: un método que protege por diseño
     * y una petición con efecto real (gasta cuota del modelo). El GET de
     * {@code /api/projects}, que no puede cubrir, lo vigila un filtro propio.
     *
     * @see com.adrian.portfolio.security.filter.CsrfValidationFilter
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ServerCsrfTokenRepository csrfTokenRepository) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // El handler por defecto (XOR) enmascara el token por petición y
                        // lo espera enmascarado de vuelta, y el cliente devuelve lo que lee
                        // de la cookie. Su motivo, BREACH, no aplica: el token no se
                        // incrusta en el HTML comprimido, viaja en un Set-Cookie.
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
