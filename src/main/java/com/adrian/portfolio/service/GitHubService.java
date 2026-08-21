package com.adrian.portfolio.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    // Topics demasiado genéricos: no dicen nada del proyecto en una tarjeta.
    private static final Set<String> NOISE_TOPICS = Set.of(
            "backend", "frontend", "full-stack", "fullstack", "fullstack-development",
            "web", "api", "app", "project", "portfolio");

    // Topics que describen QUÉ es el proyecto (arquitectura, seguridad, dominio).
    // Se muestran antes que los del stack, que ya se intuye por el lenguaje.
    private static final List<String> CONCEPT_TOPICS = List.of(
            "hexagonal-architecture", "clean-architecture", "ddd", "microservices",
            "event-driven", "cqrs", "distributed-systems", "api-gateway", "eureka",
            "resilience4j", "kafka", "jwt-authentication", "oauth2", "spring-security",
            "security", "generative-ai", "llm", "prompt-engineering",
            "artificial-intelligence", "rest-api", "api-rest", "maven-library", "junit5");

    // Muchos repos etiquetan varios sinónimos a la vez; mostrar dos del mismo
    // grupo gasta un hueco de la tarjeta sin añadir información.
    private static final List<Set<String>> SYNONYM_GROUPS = List.of(
            Set.of("generative-ai", "artificial-intelligence", "llm", "ai-client"),
            Set.of("hexagonal-architecture", "clean-architecture", "ddd"),
            Set.of("jwt-authentication", "oauth2", "spring-security", "security"),
            Set.of("rest-api", "api-rest"));

    private static final int MAX_TOPICS = 3;
    private static final int MAX_CONCEPT_TOPICS = 2;

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
        dto.setPushed_at(repo.getPushed_at());
        dto.setTopics(pickTopics(repo));

        return dto;
    }

    private List<String> pickTopics(GithubRepoResponse repo) {
        if (repo.getTopics() == null) {
            return List.of();
        }

        String language = repo.getLanguage() == null ? "" : repo.getLanguage().toLowerCase();

        List<String> relevant = repo.getTopics().stream()
                .filter(topic -> !NOISE_TOPICS.contains(topic))
                .filter(topic -> !topic.equals(language))
                .toList();

        List<String> concepts = relevant.stream()
                .filter(CONCEPT_TOPICS::contains)
                .sorted(Comparator.comparingInt(CONCEPT_TOPICS::indexOf))
                .toList();

        List<String> stack = relevant.stream()
                .filter(topic -> !CONCEPT_TOPICS.contains(topic))
                .toList();

        Set<Integer> usedGroups = new HashSet<>();
        List<String> picked = new ArrayList<>();

        // El tope de conceptuales reserva sitio para el stack: así la tarjeta dice
        // qué es el proyecto sin dejar de decir con qué está construido.
        addTopics(concepts, MAX_CONCEPT_TOPICS, usedGroups, picked);
        addTopics(stack, MAX_TOPICS, usedGroups, picked);

        return List.copyOf(picked);
    }

    private void addTopics(List<String> candidates, int limit, Set<Integer> usedGroups, List<String> picked) {
        for (String topic : candidates) {
            if (picked.size() >= limit || picked.size() >= MAX_TOPICS) {
                return;
            }

            int group = groupOf(topic);
            if (group >= 0 && !usedGroups.add(group)) {
                continue;
            }
            picked.add(topic);
        }
    }

    private int groupOf(String topic) {
        for (int i = 0; i < SYNONYM_GROUPS.size(); i++) {
            if (SYNONYM_GROUPS.get(i).contains(topic)) {
                return i;
            }
        }
        return -1;
    }

    private List<RepoDTO> getFallBackRepos() {
        return List.of(
                create("distributed-ecommerce-platform",
                        "Microservicios con Spring Boot usando Eureka, API Gateway y Circuit Breaker (Resiliece4j) con PostgreSQL",
                        "https://github.com/adrian0511/distributed-ecommerce-platform", "Java",
                        List.of("microservices", "api-gateway", "postgresql")),
                create("challengehub",
                        "ChallengeHub es una API REST desarrollada con Spring Boot que permite a los usuarios participar en retos, registrar su progreso, ganar puntos, subir de nivel y obtener insignias, utilizando autenticación JWT y control de acceso por roles.",
                        "https://github.com/adrian0511/challengehub", "Java",
                        List.of("jwt-authentication", "rest-api", "spring-boot")),
                create("biblioteca", "Sistema de gestión de biblioteca con Spring Boot",
                        "https://github.com/adrian0511/biblioteca", "Java",
                        List.of("spring-boot", "jpa")),
                create("Gestion-de-Reservas",
                        "Sistema web desarrollado con Spring Boot y MySQL para la gestión y reservas de recursos, con interfaz en HTML, CSS y JavaScript.",
                        "https://github.com/adrian0511/Gestion-de-Reservas", "JavaScript",
                        List.of("spring-boot", "mysql", "hibernate")),
                create("RetosConIA", "Proyecto de Spring Boot para generar retos educativos con IA",
                        "https://github.com/adrian0511/RetosConIA", "Java",
                        List.of("generative-ai", "spring-boot")));
    }

    // Sin pushed_at: la lista de respaldo es estática y fechar los repos sería inventar.
    private RepoDTO create(String name, String desc, String url, String lang, List<String> topics) {
        return new RepoDTO(name, desc, url, lang, topics, null);
    }

}
