package com.adrian.portfolio.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.adrian.portfolio.dto.GithubRepoResponse;
import com.adrian.portfolio.dto.RepoDTO;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class GitHubService {

    @Autowired
    private WebClient webClient;

    @Value("${github.username}")
    private String username;

    @Value("${github.token}")
    private String token;

    @Value("${github.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    private final Map<Integer, Mono<List<RepoDTO>>> cache = new ConcurrentHashMap<>();

    public Mono<List<RepoDTO>> getFeaturedRepo(int limit) {
        return cache.computeIfAbsent(limit,
                l -> fetchFeaturedRepo(l).cache(Duration.ofSeconds(cacheTtlSeconds)));
    }

    private Mono<List<RepoDTO>> fetchFeaturedRepo(int limit) {
        log.info("Cache miss: pidiendo repos a GitHub (limit={})", limit);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{username}/repos")
                        .queryParam("sort", "pushed")
                        .queryParam("per_page", limit * 2)
                        .build(username))
                .headers(headers -> {
                    if (!token.isBlank()) {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve()
                .bodyToFlux(GithubRepoResponse.class)
                .filter(repo -> !repo.getFork())
                .filter(repo -> !repo.getName().equalsIgnoreCase(username))
                .filter(repo -> repo.getDescription() != null)
                .take(limit)
                .map(this::toDTO)
                .collectList()
                .timeout(Duration.ofSeconds(7))
                .onErrorResume(error -> {
                    log.error("GitHub failed: " + error.getMessage());
                    return Mono.just(getFallBackRepos());
                });
    }

    private RepoDTO toDTO(GithubRepoResponse repo) {
        RepoDTO dto = new RepoDTO();

        dto.setName(repo.getName());
        dto.setDescription(repo.getDescription());
        dto.setHtml_url(repo.getHtml_url());
        dto.setLanguage(repo.getLanguage());

        if (repo.getTopics() != null && !repo.getTopics().isEmpty()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(repo.getTopics().size());
            dto.setTopic(repo.getTopics().get(randomIndex));
        }

        return dto;
    }

    private List<RepoDTO> getFallBackRepos() {
        return List.of(
                create("distributed-ecommerce-platform",
                        "Microservicios con Spring Boot usando Eureka, API Gateway y Circuit Breaker (Resiliece4j) con PostgreSQL",
                        "https://github.com/adrian0511/distributed-ecommerce-platform", "Java", "microservices"),
                create("challengehub",
                        "ChallengeHub es una API REST desarrollada con Spring Boot que permite a los usuarios participar en retos, registrar su progreso, ganar puntos, subir de nivel y obtener insignias, utilizando autenticación JWT y control de acceso por roles.",
                        "https://github.com/adrian0511/challengehub", "Java", "springsecurity-jwt"),
                create("biblioteca", "Sistema de gestión de biblioteca con Spring Boot",
                        "https://github.com/adrian0511/biblioteca", "Java", "springboot"),
                create("Gestion-de-Reservas",
                        "Sistema web desarrollado con Spring Boot y MySQL para la gestión y reservas de recursos, con interfaz en HTML, CSS y JavaScript.",
                        "https://github.com/adrian0511/Gestion-de-Reservas", "JavaScript", "jpa-hibernate"),
                create("RetosConIA", "Proyecto de Spring Boot para generar retos educativos con IA",
                        "https://github.com/adrian0511/RetosConIA", "Java", "generative-ai"));
    }

    private RepoDTO create(String name, String desc, String url, String lang, String topic) {
        return new RepoDTO(name, desc, url, lang, topic);
    }

}
