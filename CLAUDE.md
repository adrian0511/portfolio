# CLAUDE.md — Portfolio de Adrián Garcés

Guía para trabajar en este repositorio. Portfolio personal: **backend Spring Boot (WebFlux)** que sirve un **frontend React (Vite)** compilado y expone una pequeña API que consume la API pública de GitHub.

---

## Stack

**Backend**
- Java 21/25 (el `pom.xml` declara `java.version=21`, pero el `maven-compiler-plugin` compila con `source/target=25`; **se necesita un JDK 25 para buildear** — ver "Notas")
- Spring Boot **4.0.4** (parent)
- Spring WebFlux (reactivo, `Mono`/`Flux` — **no** Spring MVC)
- Spring Security (WebFlux security)
- Lombok
- Build: Maven (wrapper `mvnw` incluido)

**Frontend**
- React 18 + Vite (JavaScript, sin TypeScript)
- CSS global (`frontend/src/styles/global.css`, con variables `:root`); plan de pasar a CSS Modules de forma incremental
- Fuentes vía Google Fonts (JetBrains Mono, Syne)
- Se compila con Vite y se empaqueta dentro del jar (servido como estático en `/`)

---

## Estructura de carpetas

```
portfolio/
├── pom.xml                     # incluye frontend-maven-plugin (compila React en el build)
├── mvnw / mvnw.cmd             # Maven wrapper
├── frontend/                   # FRONTEND REACT
│   ├── package.json
│   ├── vite.config.js          # proxy /api -> :8080 en dev
│   ├── index.html              # entry de Vite
│   ├── public/                 # assets estáticos (img/Avatar.png, docs/CV_Adrian.pdf)
│   └── src/
│       ├── main.jsx            # monta <App>, importa global.css
│       ├── App.jsx             # compone las secciones + estado del drawer
│       ├── styles/global.css   # todo el estilo (copia evolucionada del styles.css original)
│       ├── api/client.js       # getCsrfToken() + getProjects(token)
│       ├── hooks/
│       │   ├── useRevealOnScroll.js  # IntersectionObserver -> clase .on
│       │   └── useProjects.js        # flujo csrf-token -> projects + fallback
│       └── components/
│           ├── Navbar.jsx, MobileDrawer.jsx, CustomCursor.jsx
│           ├── Hero.jsx, About.jsx, Contact.jsx, Footer.jsx
│           └── Projects.jsx, ProjectCard.jsx, GithubCard.jsx
├── src/
│   ├── main/
│   │   ├── java/com/adrian/portfolio/
│   │   │   ├── PortfolioApplication.java        # main / @SpringBootApplication
│   │   │   ├── config/AppConfig.java            # Bean WebClient (baseUrl api.github.com)
│   │   │   ├── controller/
│   │   │   │   ├── ProjectController.java       # GET /api/projects
│   │   │   │   └── CsrfTokenController.java      # GET /api/csrf-token
│   │   │   ├── service/GitHubService.java       # consume GitHub API + fallback
│   │   │   ├── dto/
│   │   │   │   ├── RepoDTO.java                 # respuesta hacia el frontend
│   │   │   │   └── GithubRepoResponse.java       # mapea la respuesta de GitHub
│   │   │   └── security/
│   │   │       ├── config/SecurityConfig.java   # WebFlux security (csrf disable, permitAll)
│   │   │       └── filter/CsrfValidationFilter.java  # valida X-CSRF-Token en /api/projects
│   │   └── resources/
│   │       └── application.properties           # (static/ ya NO existe: lo genera el build de React)
│   └── test/java/com/adrian/portfolio/
│       └── PortfolioApplicationTests.java        # context load test
└── target/                                       # build output (ignored); el jar incluye React en static/
```

---

## Cómo se levanta el proyecto

**Requisito:** un **JDK 25** disponible para `./mvnw` (ej. `export JAVA_HOME=/ruta/al/jdk-25`).

### Producción / artefacto único
`./mvnw package` compila React (vía `frontend-maven-plugin`), copia `frontend/dist` a `static/` del classpath y empaqueta todo en el jar.

```bash
./mvnw clean package
java -jar target/portfolio-0.0.1-SNAPSHOT.jar   # http://localhost:8080
```

### Desarrollo (hot-reload de React)
Backend y frontend por separado; Vite proxea `/api` al backend:

```bash
./mvnw spring-boot:run           # backend en :8080
cd frontend && npm run dev        # frontend en :5173 (o 5174 si está ocupado)
```

- Puerto backend: `server.port=${PORT:8080}`.
- El proxy de Vite (`/api` → `:8080`) hace que en dev todo sea same-origin, preservando la cookie de sesión del flujo CSRF.

### Variables / configuración (`application.properties`)
- `github.username=adrian0511`
- `github.token=${GITHUB_TOKEN:}` — opcional; si está, se usa como Bearer para subir el rate limit de la API de GitHub. Vacío = peticiones anónimas.

---

## Comunicación frontend ↔ backend

El frontend consume el backend con este flujo (ver `frontend/src/api/client.js` + `hooks/useProjects.js`):

1. **`GET /api/csrf-token`** — al montar `<Projects>`. Devuelve `{ "token": "<uuid>" }`. El token se guarda en la sesión (`ServerWebExchange` session) y el backend setea la cookie `SESSION`.
2. **`GET /api/projects`** — se llama con el header **`X-CSRF-Token: <token>`** y `credentials: 'include'` (para que viaje la cookie de sesión). Devuelve `List<RepoDTO>`.

### Endpoints

| Método | Ruta                | Auth / Header requerido        | Respuesta |
|--------|---------------------|--------------------------------|-----------|
| GET    | `/api/csrf-token`   | ninguno                        | `{ token: string }` |
| GET    | `/api/projects`     | `X-CSRF-Token` (validado en filtro) | `RepoDTO[]` |

### `RepoDTO` (contrato con el frontend)
```json
{
  "name": "string",
  "description": "string",
  "html_url": "string",
  "language": "string",
  "topic": "string"
}
```

### Detalles del backend relevantes para el frontend
- **`GitHubService`**: pide los repos del usuario a `api.github.com`, filtra forks / repo homónimo / sin descripción, toma los primeros 5, mapea a `RepoDTO`. Timeout 7s. Si GitHub falla, devuelve una **lista fallback hardcodeada** de 5 proyectos (nunca rompe).
- **`CsrfValidationFilter`** (`@Order(-100)`): intercepta **solo** `/api/projects`. Si falta el header o no coincide con el token en sesión → responde `404`. El frontend, ante error, muestra su propio fallback (estado `error` en `useProjects`).
- **`SecurityConfig`**: CSRF de Spring **deshabilitado** (se usa el filtro custom), todo `permitAll`.
- **`ProjectController`** tiene `@CrossOrigin(origins = "*")`.
- La validación CSRF depende de la **sesión** (cookie `SESSION`). En dev el proxy de Vite preserva la cookie; en el jar es same-origin y funciona directo.

### Secciones / componentes React
- `CustomCursor` — cursor custom con lag (solo mouse fino).
- `Navbar` — se vuelve sólido tras 40px de scroll; hamburguesa togglea el drawer.
- `MobileDrawer` — drawer mobile (estado en `App`, cierra con Escape / al navegar).
- `Hero` — texto + foto (con `onError` → placeholder) + chips.
- `About` — bio + grid de stack (dominado / aprendiendo / estudiando).
- `Projects` + `ProjectCard` + `GithubCard` — tarjetas desde la API con fallback.
- `Contact` — links (email, GitHub, LinkedIn, descarga CV).
- `Footer` — año dinámico (`new Date().getFullYear()`).

### Comportamiento a preservar
- Cursor personalizado con lag (solo dispositivos con mouse fino).
- Nav que se vuelve sólido tras 40px de scroll.
- Drawer mobile (hamburguesa + Escape para cerrar).
- Reveal on scroll vía `IntersectionObserver` (clase `.rv` → `.on`), en `useRevealOnScroll`.
- Carga de proyectos vía API con fallback.
- Año del copyright dinámico.

---

## Convenciones

- Paquete raíz: `com.adrian.portfolio`.
- Backend reactivo: usar `Mono`/`Flux`, **no** bloquear.
- DTOs con Lombok (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`).
- Frontend: React funcional con hooks; JavaScript (no TS); CSS global por ahora.
- Español en textos de UI y (parcialmente) comentarios.

## Notas / deuda técnica conocida
- **Java 25 requerido para buildear**: el `maven-compiler-plugin` fuerza `source/target=25` aunque `java.version=21`. Unificar (o bien a 21, o alinear todo a 25).
- Links del drawer y del `#contact` ya apuntan a los perfiles reales (`github.com/adrian0511`, `linkedin.com/in/adrdev`).
- Estilos inline aún abundantes en algunos componentes (Hero, ProjectCard, GithubCard) — candidatos a mover a CSS/Modules.
- CSS global pendiente de pasar a CSS Modules de forma incremental.
