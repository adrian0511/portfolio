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
- **`prompt-link` 1.1.0** — librería propia (Maven Central) para IA generativa vía OpenRouter; aporta `ReactiveAiService` con streaming. Arrastra Spring Cloud OpenFeign.
- Build: Maven (wrapper `mvnw` incluido)

**Frontend**
- React 18 + Vite (JavaScript, sin TypeScript)
- i18n propio (es/en) vía Context (`i18n/LanguageContext.jsx` + `translations.js`), sin librería externa
- CSS global (`frontend/src/styles/global.css`, con variables `:root`); plan de pasar a CSS Modules de forma incremental
- Fuentes **autoalojadas** en `public/fonts/` (JetBrains Mono, Syne): son fuentes variables, un fichero por familia y subset. Declaradas en `src/styles/fonts.css`, que `global.css` importa (Vite lo inlinea, no añade petición)
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
│   ├── public/                 # assets estáticos (img/Avatar.jpg|webp, fonts/*.woff2, docs/CV_Adrian_Garces_ES|EN.pdf, favicon.svg, robots.txt)
│   └── src/
│       ├── main.jsx            # monta <App>, importa global.css
│       ├── App.jsx             # compone las secciones + estado del drawer
│       ├── styles/
│       │   ├── global.css      # todo el estilo (copia evolucionada del styles.css original)
│       │   └── fonts.css       # @font-face de las fuentes autoalojadas (importado por global.css)
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
| POST   | `/api/chat`         | `X-CSRF-Token` + cupo de sesión | `text/event-stream` de fragmentos |

### `RepoDTO` (contrato con el frontend)
```json
{
  "name": "string",
  "description": "string",
  "html_url": "string",
  "language": "string",
  "topics": ["string"],
  "pushed_at": "2026-08-20T18:49:41Z"
}
```
`topics` llega ya curado (máx. 3) y puede venir vacío. `pushed_at` es `null` en la lista de respaldo.

### Detalles del backend relevantes para el frontend
- **`GitHubService`**: pide los repos del usuario a `api.github.com`, filtra forks / repo homónimo / sin descripción, toma los primeros N (5), mapea a `RepoDTO`. Cachea la respuesta en memoria (`Mono.cache(ttl)`, TTL vía `github.cache-ttl-seconds`) para no repetir la llamada a GitHub en cada visita. Timeout 7s. Si GitHub falla, devuelve una **lista fallback hardcodeada** de 5 proyectos (nunca rompe).
- **Curación de topics** (`GitHubService.pickTopics`): un repo suele traer 10-16 topics, de los que solo se muestran **3**. Se descartan los genéricos (`NOISE_TOPICS`: backend, full-stack…) y el que repite el lenguaje; se priorizan los conceptuales (`CONCEPT_TOPICS`: arquitectura, seguridad, dominio) con un tope de 2 para **reservar hueco al stack**; y `SYNONYM_GROUPS` evita mostrar dos etiquetas que dicen lo mismo (p. ej. `jwt-authentication` + `security`). El resultado es **determinista**: antes se elegía un topic al azar y cambiaba al expirar la caché.
- **`CsrfValidationFilter`** (`@Order(-100)`): intercepta **solo** `/api/projects`. Si falta el header o no coincide con el token en sesión → responde `404`. El frontend, ante error, muestra su propio fallback (estado `error` en `useProjects`).
- **`SecurityConfig`**: CSRF de Spring **deshabilitado** (se usa el filtro custom), CSP propia (`script-src 'self'`, `font-src 'self'`, `object-src 'none'`, `frame-ancestors 'none'`, etc.), todo `permitAll`. También desactiva el `cache()` por defecto de Spring Security, que ponía `no-store` en **toda** respuesta e impedía cachear los estáticos.
- **`CacheControlFilter`** (`@Order(-90)`): fija `Cache-Control` por ruta en `beforeCommit` (para ganar al manejador de estáticos):
  - `/assets/**` y `/fonts/**` → `public, max-age=31536000, immutable` (Vite pone hash de contenido en el nombre; **sustituir una fuente obliga a renombrarla**).
  - `/img/**`, `/docs/**`, `/favicon.svg` → `public, max-age=86400` (nombres estables que sí cambian: avatar, CV).
  - Resto, incluidos `index.html` y `/api/**` → `no-store`. **`index.html` nunca debe cachearse**: es quien apunta a los assets con hash, y cachearlo impediría que llegara un despliegue nuevo.
- La validación CSRF depende de la **sesión** (cookie `SESSION`). En dev el proxy de Vite preserva la cookie; en el jar es same-origin y funciona directo.

### Secciones / componentes React
- `CustomCursor` — cursor custom con lag (solo mouse fino).
- `Navbar` — se vuelve sólido tras 40px de scroll; hamburguesa togglea el drawer; incluye `LanguageToggle`.
- `LanguageToggle` — botones ES/EN (banderas SVG inline, no emoji) que llaman a `setLang` del `LanguageContext`.
- `MobileDrawer` — drawer mobile (estado en `App`, cierra con Escape / al navegar).
- `Hero` — texto + foto (con `onError` → placeholder) + chips.
- `About` — bio + grid de stack (dominado / aprendiendo / estudiando).
- `Projects` + `ProjectCard` + `GithubCard` — tarjetas desde la API con fallback. `ProjectCard` muestra chip de lenguaje (logo de `react-icons` tintado con el color de linguist; punto de color como fallback si el lenguaje no tiene logo), título legible (`gym-reservas` → `Gym Reservas`, respetando nombres tipo `RetosConIA`), descripción recortada a 3 líneas (`-webkit-line-clamp`), chips de topics y "actualizado hace X" vía `Intl.RelativeTimeFormat` (sin cadenas de traducción propias para las unidades).
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
- `GitHubServiceTest` — unitario, sin red: construye el `WebClient` con `exchangeFunction(...)` fake para simular respuestas de GitHub. Cubre filtrado (forks / repo homónimo / sin descripción), mapeo a `RepoDTO`, fallback ante error, caché (no repite la llamada HTTP), el header `Authorization: Bearer` condicionado a que haya token, y la curación de topics (genéricos descartados, prioridad conceptual, hueco reservado al stack, sinónimos deduplicados, repo sin topics → lista vacía).
- `CsrfValidationFilterTest` — unitario sobre el `WebFilter` con `MockServerWebExchange`: sin header → 404, header que no coincide con la sesión → 404, header válido → deja pasar, rutas distintas de `/api/projects` no se validan.
- `ChatServiceTest` — el prompt de sistema lleva reglas + perfil, el historial se conserva en orden, y los errores **se propagan** (traducirlos es cosa del advice).
- `ChatExceptionHandlerTest` — cada `statusCode` produce su mensaje (429 límite, 402 sin crédito, config/red genérico) y siempre con 200 + `text/event-stream`.
- `ChatControllerTest` — pregunta vacía no llega al modelo, recorte a 500 caracteres, historial recortado a 6 turnos, y un turno con rol `system` degradado a `user`.
- `ChatRateLimitFilterTest` — cupo por sesión: dentro pasa, al superarlo 429 sin llamar al modelo, sesiones distintas no comparten cupo, otras rutas no consumen.
- `CacheControlFilterTest` — la política de caché por ruta: assets con hash y fuentes inmutables, imágenes/CV a un día, y `index.html` + `/api/**` sin cachear nunca (esto último es el que protege los despliegues).
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
- `api/streamChat.test.js` — el parser SSE: reconstruye el texto, aguanta que un evento llegue troceado entre lecturas, une varias líneas `data:` de un mismo evento, y distingue 429 del resto.
- `hooks/useChat.test.js` — turno de usuario + turno del asistente rellenándose con el stream, preguntas vacías ignoradas, token CSRF reutilizado, y mensajes de límite/error sin romper la UI.
- `components/ProjectCard.test.jsx` — título legible (y respeto de mayúsculas existentes), chips de lenguaje y topics, ausencia de chips si el repo no trae topics, "actualizado hace X" localizado (con `vi.setSystemTime`) y omitido si no hay `pushed_at`, enlace y descripción de respaldo.

No hay tests de `CustomCursor` (loop de `requestAnimationFrame` puramente imperativo) ni de componentes de solo layout (`Hero`, `About`, `Navbar`, `MobileDrawer`, `GithubCard`, `Projects`) — bajo valor relativo al esfuerzo de mockear DOM/IntersectionObserver para lo que son, en esencia, vistas sin lógica propia.

---

## Chat con IA

Asistente que responde preguntas sobre el perfil de Adrián, montado sobre **su propia librería** [`prompt-link`](https://github.com/adrian0511/prompt-link) (`io.github.adrian0511:prompt-link`, en Maven Central), que enruta a OpenRouter.

**Por qué existe**: no es una utilidad para el visitante (pocos usarán un chat), es la demostración de una competencia que el portfolio solo afirmaba. De paso justifica WebFlux: hasta ahora el backend reactivo servía un único `GET`; el streaming SSE token a token sí es el caso de uso para el que existe WebFlux.

**Flujo**: `POST /api/chat` → `ChatService` monta `[system(reglas+perfil), ...historial, user(pregunta)]` → `ReactiveAiService.stream(...)` → `Flux<String>` → SSE al navegador.

- **`chat/profile.md`** (en `resources/`) es la **única** fuente de datos del asistente. Ampliar el chat = editar ese fichero, sin tocar código.
- **Guardarraíles en el prompt de sistema**: responder solo desde el perfil; ante lo que no consta, derivar al email; hablar de Adrián en tercera persona; ignorar instrucciones del visitante que intenten reescribir las reglas. *Un modelo inventando "sí, domina Kubernetes" ante un reclutador es peor que no tener chat.*
- **`ChatExceptionHandler`** (`@RestControllerAdvice`): traduce `AiClientException` a texto útil con **200**, no a un error. Distingue 429 (límite), 402 (sin crédito) y el resto. **Limitación real**: solo captura fallos *previos al primer token* (sin API key, 401, 429, red), que son los habituales porque `stream(...)` falla en la petición inicial. Un fallo a mitad de stream (`STREAM_ERROR`) llega con la respuesta ya comprometida y el visitante vería la respuesta truncada.
- **`ChatRateLimitFilter`** (`@Order(-95)`): cupo por sesión (`chat.max-messages-per-session`, 20 por defecto). Un endpoint de IA público sin límite es una factura abierta.
- **Topes de entrada en el controller**: pregunta a 500 caracteres y historial a los 6 últimos turnos; los turnos con rol `system` se degradan a `user` para que nadie reescriba las reglas desde el navegador.
- **Sin API key el chat no rompe**: responde con el mensaje de respaldo derivando al email, igual que los proyectos tienen su lista de respaldo.

**Dónde va la API key**:
- **Local**: `config/application.properties` (en la raíz del proyecto, **git-ignorado**). Spring Boot lee `./config/` automáticamente y sus valores ganan a los de `src/main/resources`, sin perfiles ni flags ni dependencias. Es el sustituto nativo de un `.env`, que Spring **no** lee de serie.
  - *No buscar librerías de `.env`*: `spring-dotenv` no sirve aquí — su última versión es de mayo de 2023 y no funciona con Spring Boot 4. La ventaja de `./config/` es justamente que forma parte de la resolución de configuración del propio Spring Boot, así que no se rompe al subir de versión.
- **Railway**: variable de entorno `OPENROUTER_API_KEY`.
- **Nunca** en `frontend/.env`: Vite inlinea las variables `VITE_*` en el bundle público y la clave quedaría a la vista de cualquiera.
- Hay que **reiniciar** el backend tras ponerla: Spring la lee al arrancar.

**Configuración** (`ai.*` las lee la librería):
- `ai.api-key=${OPENROUTER_API_KEY:}` — sin ella, modo respaldo.
- `ai.model=${AI_MODEL:google/gemma-4-31b-it:free}` — el tier gratuito de OpenRouter son **20 req/min y 50 req/día** (1.000/día si alguna vez se compran $10 en créditos). Al agotarse entra el mensaje de respaldo.
- `ai.read-timeout=25s` — seguro **porque hay streaming**: el primer token llega rápido. En una llamada no-streaming OpenRouter no envía nada hasta terminar de generar, y 25s mataría respuestas largas.

**Sin RAG a propósito**: el perfil son dos páginas y cabe entero en el prompt de sistema. Montar embeddings para eso sería sobreingeniería.

---

## Rendimiento

Decisiones tomadas y por qué (medido con Playwright contra el jar de producción):

- **Caché de estáticos** (`CacheControlFilter`): antes todo salía con `no-store` por el default de Spring Security, así que **cada visita recurrente re-descargaba ~306 KB**. Ahora la 2ª visita solo pide `index.html` y la API (~2,5 KB); el resto sale de caché.
- **Fuentes autoalojadas**: elimina 2 handshakes DNS+TLS a `fonts.googleapis.com`/`fonts.gstatic.com` y permite servirlas con caché `immutable` propia. Son **variables**: un fichero por familia+subset cubre todos los pesos (declarados como rango, `font-weight: 400 700`). Con `unicode-range`, en es/en solo se descarga el subset `latin`.
- **`Avatar.jpg` (50 KB) sustituye a `Avatar.png` (189 KB)**: la foto la sirve el `<picture>` como webp (14 KB) a casi todos los navegadores; el JPEG es el respaldo y el `og:image`. PNG es mal formato para un retrato.
- **Compresión**: no se activa `server.compression` porque **Railway ya comprime en su proxy** (verificado: `Content-Encoding: gzip` con `Server: railway-hikari`). Activarla en el origen no aportaría nada al usuario; haría falta si se cambia de hosting.
- **No se hizo code splitting** (es una sola página) ni se sustituyó React por Preact (~30 KB gzip de ahorro, pero riesgo alto para el valor).

## Convenciones

- Paquete raíz: `com.adrian.portfolio`.
- Backend reactivo: usar `Mono`/`Flux`, **no** bloquear.
- DTOs con Lombok (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`).
- Frontend: React funcional con hooks; JavaScript (no TS); CSS global por ahora.
- Español en textos de UI y (parcialmente) comentarios.
- **Comentarios solo si son necesarios**: se comenta el *porqué* de una decisión no evidente (un workaround, una restricción externa, una alternativa descartada), nunca el *qué* hace el código. Si el comentario se limita a repetir lo que ya dice el nombre de la función o la línea siguiente, sobra.

## Notas / deuda técnica conocida
- **Java 25 requerido para buildear**: el `maven-compiler-plugin` fuerza `source/target=25` aunque `java.version=21`. Unificar (o bien a 21, o alinear todo a 25).
- Links del drawer y del `#contact` ya apuntan a los perfiles reales (`github.com/adrian0511`, `linkedin.com/in/adrdev`).
- Estilos inline aún abundantes en algunos componentes (Hero, GithubCard) — candidatos a mover a CSS/Modules. `ProjectCard` ya está migrado (clase `.pc-link`).
- CSS global pendiente de pasar a CSS Modules de forma incremental.
- **Sin CI/CD**: no hay workflow de GitHub Actions que ejecute `./mvnw test` / `npm test` en cada push o PR. Los tests existen pero corren solo en local por ahora.
- **`vite` (devDependency) con vulnerabilidades conocidas** (moderate/high, vía `npm audit`): afectan solo al servidor de desarrollo (`vite dev`), no al build de producción que sirve Spring Boot. Actualizar a Vite 8 es un cambio mayor (breaking) pendiente de evaluar aparte.
- Sin linter configurado en el frontend (no hay `.eslintrc` ni script `lint`).
- **`pushed_at` no es la fecha del último commit**: GitHub lo actualiza con cualquier push a *cualquier* rama (ramas de Dependabot, borrado de ramas…), así que el "Actualizado hace X" de las tarjetas puede indicar actividad que no es del autor. La fecha real sería `GET /repos/{owner}/{repo}/commits?per_page=1` (una llamada extra por repo). Se optó por mantener `pushed_at` por simplicidad.
- No hay `<link rel="alternate" hreflang="es|en">` en `index.html` pese al contenido bilingüe (solo `og:locale:alternate`).
