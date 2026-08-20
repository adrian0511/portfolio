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
- i18n propio (es/en) vía Context (`i18n/LanguageContext.jsx` + `translations.js`), sin librería externa
- CSS global (`frontend/src/styles/global.css`, con variables `:root`); plan de pasar a CSS Modules de forma incremental
- Fuentes vía Google Fonts (JetBrains Mono, Syne)
- Tests: Vitest + @testing-library/react + jsdom
- Se compila con Vite y se empaqueta dentro del jar (servido como estático en `/`)

---

## Estructura de carpetas

```
portfolio/
├── pom.xml                     # incluye frontend-maven-plugin (compila React en el build)
├── mvnw / mvnw.cmd             # Maven wrapper
├── Dockerfile                  # build multi-stage (JDK -> JRE), usuario no-root
├── frontend/                   # FRONTEND REACT
│   ├── package.json            # scripts: dev, build, preview, test, test:watch
│   ├── vite.config.js          # proxy /api -> :8080 en dev + plugin SEO (sitemap, BUILD_TIME)
│   ├── vitest.config.js        # config de tests (jsdom + @testing-library)
│   ├── index.html              # entry de Vite (meta SEO, Open Graph, JSON-LD)
│   ├── public/                 # assets estáticos (img/Avatar.png|webp, docs/CV_Adrian_Garces_ES|EN.pdf, favicon.svg, robots.txt)
│   └── src/
│       ├── main.jsx            # monta <App>, importa global.css
│       ├── App.jsx             # compone las secciones + estado del drawer
│       ├── styles/global.css   # todo el estilo (copia evolucionada del styles.css original)
│       ├── api/
│       │   ├── client.js       # getCsrfToken() + getProjects(token)
│       │   └── client.test.js
│       ├── i18n/
│       │   ├── LanguageContext.jsx    # LanguageProvider/useLanguage: detecta/persiste es|en
│       │   ├── LanguageContext.test.jsx
│       │   └── translations.js        # diccionarios es/en (texto con **negrita** para RichText)
│       ├── hooks/
│       │   ├── useRevealOnScroll.js       # IntersectionObserver -> clase .on
│       │   ├── useRevealOnScroll.test.jsx
│       │   ├── useProjects.js             # flujo csrf-token -> projects + fallback
│       │   └── useProjects.test.js
│       ├── test/setup.js       # setup de Vitest (matchers de @testing-library/jest-dom)
│       └── components/
│           ├── Navbar.jsx, MobileDrawer.jsx, CustomCursor.jsx, LanguageToggle.jsx
│           ├── Hero.jsx, About.jsx, Contact.jsx, Footer.jsx
│           ├── Projects.jsx, ProjectCard.jsx, GithubCard.jsx
│           ├── RichText.jsx    # **texto** -> <strong>, sin dangerouslySetInnerHTML
│           └── *.test.jsx      # tests de RichText, LanguageToggle, Contact, Footer
├── src/
│   ├── main/
│   │   ├── java/com/adrian/portfolio/
│   │   │   ├── PortfolioApplication.java        # main / @SpringBootApplication
│   │   │   ├── config/AppConfig.java            # Bean WebClient (baseUrl api.github.com)
│   │   │   ├── controller/
│   │   │   │   ├── ProjectController.java       # GET /api/projects
│   │   │   │   └── CsrfTokenController.java      # GET /api/csrf-token
│   │   │   ├── service/GitHubService.java       # consume GitHub API + cache + fallback
│   │   │   ├── dto/
│   │   │   │   ├── RepoDTO.java                 # respuesta hacia el frontend
│   │   │   │   └── GithubRepoResponse.java       # mapea la respuesta de GitHub
│   │   │   └── security/
│   │   │       ├── config/SecurityConfig.java   # WebFlux security (csrf disable, CSP, permitAll)
│   │   │       └── filter/CsrfValidationFilter.java  # valida X-CSRF-Token en /api/projects
│   │   └── resources/
│   │       └── application.properties           # (static/ ya NO existe: lo genera el build de React)
│   └── test/java/com/adrian/portfolio/
│       ├── PortfolioApplicationTests.java        # context load test
│       ├── CsrfFlowIntegrationTest.java          # flujo csrf-token -> projects de punta a punta
│       ├── controller/CsrfTokenControllerTest.java
│       ├── controller/ProjectControllerTest.java
│       ├── security/filter/CsrfValidationFilterTest.java
│       └── service/GitHubServiceTest.java        # WebClient con exchangeFunction fake (sin red)
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
- `github.cache-ttl-seconds=${GITHUB_CACHE_TTL:600}` — TTL de la caché en memoria de `GitHubService` (evita repetir la llamada a GitHub en cada carga de `/api/projects`).
- `server.forward-headers-strategy=framework` — necesario porque Railway termina el TLS en su proxy; sin esto la app ve las peticiones como HTTP y Spring Security no emite HSTS.

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
- **`GitHubService`**: pide los repos del usuario a `api.github.com`, filtra forks / repo homónimo / sin descripción, toma los primeros N (5), mapea a `RepoDTO`. Cachea la respuesta en memoria (`Mono.cache(ttl)`, TTL vía `github.cache-ttl-seconds`) para no repetir la llamada a GitHub en cada visita. Timeout 7s. Si GitHub falla, devuelve una **lista fallback hardcodeada** de 5 proyectos (nunca rompe).
- **`CsrfValidationFilter`** (`@Order(-100)`): intercepta **solo** `/api/projects`. Si falta el header o no coincide con el token en sesión → responde `404`. El frontend, ante error, muestra su propio fallback (estado `error` en `useProjects`).
- **`SecurityConfig`**: CSRF de Spring **deshabilitado** (se usa el filtro custom), CSP propia (`script-src 'self'`, `object-src 'none'`, `frame-ancestors 'none'`, etc.), todo `permitAll`.
- La validación CSRF depende de la **sesión** (cookie `SESSION`). En dev el proxy de Vite preserva la cookie; en el jar es same-origin y funciona directo.

### Secciones / componentes React
- `CustomCursor` — cursor custom con lag (solo mouse fino).
- `Navbar` — se vuelve sólido tras 40px de scroll; hamburguesa togglea el drawer; incluye `LanguageToggle`.
- `LanguageToggle` — botones ES/EN (banderas SVG inline, no emoji) que llaman a `setLang` del `LanguageContext`.
- `MobileDrawer` — drawer mobile (estado en `App`, cierra con Escape / al navegar).
- `Hero` — texto + foto (con `onError` → placeholder) + chips.
- `About` — bio + grid de stack (dominado / aprendiendo / estudiando).
- `Projects` + `ProjectCard` + `GithubCard` — tarjetas desde la API con fallback.
- `Contact` — links (email, GitHub, LinkedIn, descarga de CV según idioma activo).
- `Footer` — año dinámico (`new Date().getFullYear()`).
- `RichText` — convierte `**texto**` en `<strong>` construyendo nodos React (sin `dangerouslySetInnerHTML`).

### i18n (`i18n/LanguageContext.jsx` + `i18n/translations.js`)
- `LanguageProvider` detecta el idioma inicial en este orden: preferencia guardada en `localStorage` (`lang`) → `navigator.language`/`navigator.languages` (es si empieza por "es") → inglés por defecto.
- `setLang` valida contra `SUPPORTED = ['es', 'en']`, persiste en `localStorage` y sincroniza `document.documentElement.lang`.
- Todo el texto de UI vive en `translations.js` (diccionarios `es`/`en`); el texto con `**negrita**` se renderiza con `RichText`.

### Comportamiento a preservar
- Cursor personalizado con lag (solo dispositivos con mouse fino).
- Nav que se vuelve sólido tras 40px de scroll.
- Drawer mobile (hamburguesa + Escape para cerrar).
- Reveal on scroll vía `IntersectionObserver` (clase `.rv` → `.on`), en `useRevealOnScroll`.
- Carga de proyectos vía API con fallback.
- Año del copyright dinámico.
- Selector de idioma ES/EN con persistencia en `localStorage` y detección por navegador.

---

## Testing

### Backend (JUnit 5 + Mockito + WebTestClient + reactor-test, todo offline)
```bash
./mvnw test
```
- `GitHubServiceTest` — unitario, sin red: construye el `WebClient` con `exchangeFunction(...)` fake para simular respuestas de GitHub. Cubre filtrado (forks / repo homónimo / sin descripción), mapeo a `RepoDTO`, fallback ante error, caché (no repite la llamada HTTP) y el header `Authorization: Bearer` condicionado a que haya token.
- `CsrfValidationFilterTest` — unitario sobre el `WebFilter` con `MockServerWebExchange`: sin header → 404, header que no coincide con la sesión → 404, header válido → deja pasar, rutas distintas de `/api/projects` no se validan.
- `CsrfTokenControllerTest` — `WebTestClient.bindToController(...)`: token no vacío + cookie de sesión, mismo token en la misma sesión, tokens distintos entre sesiones.
- `ProjectControllerTest` — `WebTestClient.bindToController(...)` con `GitHubService` mockeado: 200 con la lista, 200 con lista vacía, y el camino defensivo 204 (`Mono.empty()`) del controller.
- `CsrfFlowIntegrationTest` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureWebTestClient`, con `GitHubService` reemplazado por `@MockitoBean`: valida el flujo real csrf-token → cookie + header → `/api/projects`, incluidos los caminos 404 (sin header, o token de otra sesión).

### Frontend (Vitest + @testing-library/react + jsdom)
```bash
cd frontend
npm test          # una pasada (CI)
npm run test:watch
```
- `api/client.test.js` — `getCsrfToken`/`getProjects` contra `fetch` mockeado: headers/credentials correctos, error si la respuesta no es `ok`, `[]` en `204`.
- `hooks/useProjects.test.js` — estados `loading` → `success`/`error` mockeando `api/client.js`.
- `hooks/useRevealOnScroll.test.jsx` — `IntersectionObserver` mockeado: observa los `.rv` al montar, añade `.on` al intersectar, `disconnect()` al desmontar.
- `i18n/LanguageContext.test.jsx` — detección de idioma (`localStorage` > navegador > default), `setLang` (persistencia, `<html lang>`, idiomas no soportados), error al usar `useLanguage` fuera del provider.
- `components/RichText.test.jsx`, `LanguageToggle.test.jsx`, `Contact.test.jsx`, `Footer.test.jsx` — comportamiento observable: negritas → `<strong>`, botón de idioma activo/click, CV descargable por idioma, año dinámico.

No hay tests de `CustomCursor` (loop de `requestAnimationFrame` puramente imperativo) ni de componentes de solo layout (`Hero`, `About`, `Navbar`, `MobileDrawer`, `ProjectCard`, `GithubCard`, `Projects`) — bajo valor relativo al esfuerzo de mockear DOM/IntersectionObserver para lo que son, en esencia, vistas sin lógica propia.

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
- **Sin CI/CD**: no hay workflow de GitHub Actions que ejecute `./mvnw test` / `npm test` en cada push o PR. Los tests existen pero corren solo en local por ahora.
- **`vite` (devDependency) con vulnerabilidades conocidas** (moderate/high, vía `npm audit`): afectan solo al servidor de desarrollo (`vite dev`), no al build de producción que sirve Spring Boot. Actualizar a Vite 8 es un cambio mayor (breaking) pendiente de evaluar aparte.
- Sin linter configurado en el frontend (no hay `.eslintrc` ni script `lint`).
- No hay `<link rel="alternate" hreflang="es|en">` en `index.html` pese al contenido bilingüe (solo `og:locale:alternate`).
