package com.adrian.portfolio.controller;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfTokenControllerTest {

    private static final ParameterizedTypeReference<Map<String, String>> TOKEN_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebTestClient client = WebTestClient.bindToController(new CsrfTokenController()).build();

    private EntityExchangeResult<Map<String, String>> requestToken(String cookieName, String cookieValue) {
        WebTestClient.RequestHeadersSpec<?> request = client.get().uri("/api/csrf-token");
        if (cookieName != null) {
            request = request.cookie(cookieName, cookieValue);
        }
        return request.exchange().expectStatus().isOk().expectBody(TOKEN_TYPE).returnResult();
    }

    @Test
    void devuelveUnTokenYUnaCookieDeSesion() {
        EntityExchangeResult<Map<String, String>> result = requestToken(null, null);

        assertThat(result.getResponseBody().get("token")).isNotBlank();
        assertThat(result.getResponseHeaders().get(HttpHeaders.SET_COOKIE)).isNotEmpty();
    }

    @Test
    void enLaMismaSesionDevuelveSiempreElMismoToken() {
        EntityExchangeResult<Map<String, String>> first = requestToken(null, null);
        String[] cookiePair = firstCookiePair(first);

        EntityExchangeResult<Map<String, String>> second = requestToken(cookiePair[0], cookiePair[1]);

        assertThat(second.getResponseBody().get("token")).isEqualTo(first.getResponseBody().get("token"));
    }

    @Test
    void enSesionesDistintasDevuelveTokensDistintos() {
        String tokenA = requestToken(null, null).getResponseBody().get("token");
        String tokenB = requestToken(null, null).getResponseBody().get("token");

        assertThat(tokenA).isNotEqualTo(tokenB);
    }

    private String[] firstCookiePair(EntityExchangeResult<Map<String, String>> result) {
        String setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        return setCookie.split(";")[0].split("=", 2);
    }
}
