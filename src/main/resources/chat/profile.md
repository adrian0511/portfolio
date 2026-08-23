# Perfil de Adrián Garcés

Este fichero es la ÚNICA fuente de datos del chat de la web. Todo lo que el
asistente pueda afirmar tiene que estar escrito aquí. Ampliarlo es la forma de
que el chat sepa más; no hace falta tocar código.

## Registro y tono del asistente

Quien pregunta suele ser un reclutador o alguien evaluando su trabajo, así que
la respuesta se lee como una referencia profesional, no como una charla:

- Directo y concreto. Nada de entusiasmo impostado, emojis ni exclamaciones.
- Se responde con hechos verificables del perfil, no con adjetivos vacíos:
  mejor "publicó una librería en Maven Central" que "es muy bueno".
- Si algo no consta, se dice con naturalidad y se ofrece el email. Reconocer un
  límite da más credibilidad que rellenar el hueco.
- No se exagera el nivel: lo que está en "aprendiendo" se presenta como tal.

## Identidad

- Nombre completo: Adrián Arsenio Garcés Jiménez
- Se le conoce como: Adrián Garcés, adrian0511, adrdev
- Rol: Desarrollador backend
- Web: https://adrian0511.dev
- GitHub: https://github.com/adrian0511
- LinkedIn: https://linkedin.com/in/adrdev
- Email: adriangarces0310@gmail.com
- Idiomas de trabajo: español e inglés (su documentación técnica y varios de sus
  proyectos están escritos en inglés).

## Situación actual

Abierto a ofertas laborales y a prácticas. La vía de contacto para cualquier
propuesta, entrevista o detalle que no aparezca en este perfil es el email:
adriangarces0310@gmail.com.

## Formación

- **Bachillerato** completado.
- Desde **septiembre de 2026** cursa el ciclo formativo de grado superior de
  **Desarrollo de Aplicaciones Web (DAW)**, que sigue estudiando actualmente.

Su formación técnica en backend es en gran parte autodidacta y está respaldada
por proyectos públicos y auditables en GitHub, no solo por temario.

## Enfoque profesional

Desarrollador backend centrado en construir APIs robustas y sistemas bien
diseñados con Java y Spring. Le importa que el software sea escalable,
resiliente y fácil de mantener.

Trabaja arquitecturas de microservicios y sistemas distribuidos (Spring Cloud,
API Gateway, Eureka, Kafka, Resilience4j) con foco en seguridad (Spring
Security, OAuth2). En paralelo se expande hacia Python (FastAPI) y Node.js
(NestJS) para no atarse a un solo ecosistema.

Busca un equipo donde seguir creciendo y aportar valor desde el primer día.

## Cómo trabaja

Rasgos que se pueden comprobar en el código de sus repositorios públicos:

- **Escribe tests.** No como añadido: este portfolio tiene más de 100 tests
  entre backend (JUnit 5, Mockito, WebTestClient) y frontend (Vitest,
  Testing Library), y `cookbook` cubre todas sus capas.
- **Diseña antes de teclear.** Usa arquitectura hexagonal en `cookbook` y
  separación por capas en el resto; no mezcla lógica de negocio con infraestructura.
- **Piensa en el fallo.** Sus servicios llevan timeouts, cachés y respuestas de
  respaldo: si una dependencia externa cae, la aplicación degrada en vez de
  romperse.
- **Se preocupa por la seguridad.** Validación CSRF por sesión, cabeceras CSP,
  límites de uso por IP y gestión de secretos fuera del repositorio.
- **Automatiza.** Integración continua en GitHub Actions y despliegue con Docker.

## Stack

Dominado: Java, Spring Boot, Spring Security, REST APIs, PostgreSQL, MySQL,
Kafka, Docker, Linux, Git y GitHub.

Microservicios y sistemas distribuidos: Spring Cloud, API Gateway, Eureka,
Resilience4j, OAuth2.

Aprendiendo actualmente: Python, FastAPI, SQLAlchemy, Pydantic, NestJS,
TypeScript, React, Sequelize.

## Proyectos

**prompt-link** — Librería Java publicada en **Maven Central**
(`io.github.adrian0511:prompt-link`): cliente de IA generativa sobre OpenRouter
con autoconfiguración de Spring Boot, cliente Feign aislado para que la API key
no se filtre a otros clientes, errores tipados y streaming de tokens en WebFlux.
Es software de terceros listo para usar, no un ejercicio: es la librería que
mueve este mismo chat.

**Portfolio** (esta web) — Backend Spring Boot con WebFlux que sirve un frontend
React compilado y consume la API de GitHub. Flujo CSRF por sesión, caché en
memoria con TTL, política de caché por ruta, fuentes autoalojadas y este
asistente con streaming SSE.

**finance-tracker** — Backend en Spring Boot para gestión de finanzas
personales con integración de IA, autenticación JWT y PostgreSQL.

**cookbook** — Gestor de recetas full-stack en Java 21 con Spring Boot y
arquitectura hexagonal, React con Vite y Tailwind, tests en todas las capas y
despliegue con Docker.

**gym-reservas** — Sistema de reservas para gimnasios con NestJS, React,
PostgreSQL y autenticación JWT.

**bug-hunt** — Acortador de URLs en Python con FastAPI y SQLite.

## Cosas que este perfil NO contiene

No hay información sobre: años de experiencia profesional, empresas en las que
haya trabajado, pretensiones salariales, disponibilidad geográfica ni datos
personales más allá de los de contacto. Tampoco hay más formación que la
recogida arriba.

Ante cualquiera de estos temas, la respuesta correcta es decir que no consta en
el perfil e invitar a escribir a adriangarces0310@gmail.com, sin especular ni
estimar.
