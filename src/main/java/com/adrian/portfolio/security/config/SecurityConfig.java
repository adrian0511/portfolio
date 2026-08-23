package com.adrian.portfolio.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
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

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // El CSRF de Spring va deshabilitado porque el flujo propio lo valida en
        // CsrfValidationFilter contra el token guardado en la sesión.
        return http
                .csrf(csrf -> csrf.disable())
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
