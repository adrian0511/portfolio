# CLAUDE.md — Portfolio de Adrián Garcés

Guía para trabajar en este repositorio. Portfolio personal: **backend Spring Boot (WebFlux)** que sirve un **frontend React (Vite)** compilado y expone una pequeña API que consume la API pública de GitHub.

---

## Stack

**Backend**
- **Java 25** (`java.version=25`; el Dockerfile usa temurin 25). Se necesita un JDK 25 para buildear.
- Spring Boot **4.0.8** (parent) — **fijado a la línea 4.0.x**, ver "Notas"
- Spring WebFlux (reactivo, `Mono`/`Flux` — **no** Spring MVC)
- Spring Security (WebFlux security)
- Lombok
- **`prompt-link` 1.2.0** — librería propia (Maven Central) para IA generativa vía OpenRouter; aporta `ReactiveAiService` con streaming, modelos de respaldo (`ai.models`) y control del razonamiento (`ai.reasoning.*`). Arrastra Spring Cloud OpenFeign. El código vive en `../prompt-link`; desde la 1.2.0 se firma con la clave GPG `06BD4A7D4F184D5B` (la de la 1.1.0 se perdió).
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
├── .github/workflows/ci.yml    # tests de backend y frontend + package en cada push/PR
├── Dockerfile                  # build multi-stage (JDK -> JRE), usuario no-root
├── cv/                         # fuente de los PDF del CV (cv-es|en.html, cv.css, render.ps1)
├── frontend/                   # FRONTEND REACT
│   ├── package.json            # scripts: dev, build, preview, test, test:watch, lint
│   ├── eslint.config.js        # flat config + react-hooks
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
│       │   ├── client.js       # ensureCsrfCookie() + getProjects(token) + streamChat()
│       │   └── client.test.js
│       ├── i18n/
│       │   ├── LanguageContext.jsx    # LanguageProvider/useLanguage: detecta/persiste es|en
│       │   ├── LanguageContext.test.jsx
│       │   └── translations.js        # diccionarios es/en (texto con **negrita** para RichText)
│       ├── hooks/
│       │   ├── useRevealOnScroll.js       # IntersectionObserver -> clase .on
│       │   ├── useRevealOnScroll.test.jsx
│       │   ├── useProjects.js             # flujo csrf-token -> projects + fallback
│       │   ├── useProjects.test.js
│       │   ├── useChat.js                 # turnos + stream + AbortController (cancel)
│       │   └── useChat.test.js
│       ├── test/setup.js       # setup de Vitest (matchers de @testing-library/jest-dom)
│       └── components/
│           ├── Navbar.jsx, MobileDrawer.jsx, CustomCursor.jsx, LanguageToggle.jsx
│           ├── Hero.jsx, About.jsx, Contact.jsx, Footer.jsx
│           ├── Projects.jsx, ProjectCard.jsx, GithubCard.jsx
│           ├── Chat.jsx        # lanzador + panel; el estado abierto/cerrado vive en App
│           ├── ChatText.jsx    # markdown ligero del modelo (negritas, viñetas) -> nodos React
│           ├── RichText.jsx    # **texto** -> <strong>, sin dangerouslySetInnerHTML
│           └── *.test.jsx      # tests de RichText, ChatText, LanguageToggle, Contact, Footer,
│                               # MobileDrawer, Chat, ProjectCard
├── src/
│   ├── main/
│   │   ├── java/com/adrian/portfolio/
│   │   │   ├── PortfolioApplication.java        # main / @SpringBootApplication
│   │   │   ├── config/AppConfig.java            # Bean WebClient (baseUrl api.github.com)
│   │   │   ├── controller/
│   │   │   │   ├── ProjectController.java       # GET /api/projects
│   │   │   │   ├── CsrfTokenController.java      # GET /api/csrf-token
│   │   │   │   ├── ChatController.java          # POST /api/chat + topes de entrada
│   │   │   │   └── ChatExceptionHandler.java    # fallos del modelo -> texto util con 200
│   │   │   ├── service/
│   │   │   │   ├── GitHubService.java           # consume GitHub API + cache + fallback
│   │   │   │   └── ChatService.java             # prompt de sistema (reglas + perfil + repos)
│   │   │   ├── dto/
│   │   │   │   ├── RepoDTO.java                 # respuesta hacia el frontend
│   │   │   │   ├── GithubRepoResponse.java       # mapea la respuesta de GitHub
│   │   │   │   └── ChatRequest.java             # pregunta + historial que manda el navegador
│   │   │   └── security/
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java           # WebFlux security (CSRF nativo, CSP, permitAll)
│   │   │       │   └── TrustedClientIpTransformer.java  # IP real del visitante (X-Forwarded-For)
│   │   │       └── filter/
│   │   │           ├── CsrfValidationFilter.java     # (-99) valida X-XSRF-TOKEN en GET /api/projects
│   │   │           ├── ChatRateLimitFilter.java      # (-95) cupos del chat: sesion, red/hora, red/dia, global
│   │   │           ├── CacheControlFilter.java       # (-90) Cache-Control por ruta
│   │   │           └── CsrfCookieFilter.java         # (el ultimo) fuerza la cookie XSRF-TOKEN en /api/**
│   │   └── resources/
│   │       ├── application.properties           # (static/ ya NO existe: lo genera el build de React)
│   │       └── chat/profile.md                  # lo que el asistente puede afirmar de Adrian
│   └── test/java/com/adrian/portfolio/
│       ├── PortfolioApplicationTests.java        # context load test
│       ├── CsrfFlowIntegrationTest.java          # flujo csrf-token -> projects de punta a punta
│       ├── ChatCsrfIntegrationTest.java          # CSRF nativo sobre POST /api/chat
│       ├── controller/ProjectControllerTest.java
│       ├── controller/ChatControllerTest.java, ChatExceptionHandlerTest.java
│       ├── security/config/TrustedClientIpTransformerTest.java
│       ├── security/filter/CsrfValidationFilterTest.java
│       ├── security/filter/ChatRateLimitFilterTest.java, CacheControlFilterTest.java
│       ├── service/ChatServiceTest.java
│       └── service/GitHubServiceTest.java        # WebClient con exchangeFunction fake (sin red)
└── target/                                       # build output (ignored); el jar incluye React en static/
```

---

## Flujo de trabajo con git

**Todo cambio se hace en `dev` y llega a `main` por merge.** Nunca se commitea
directamente en `main`.

```bash
git checkout dev
git merge main            # partir de lo ultimo publicado
# ... trabajo, commits ...
git push origin dev       # el CI corre sobre dev

git checkout main
git merge dev             # merge, no rebase ni cherry-pick
git push origin main      # Railway despliega desde main
```

`main` es la rama que despliega: lo que entra ahí sale a producción en cuanto
Railway lo detecta. El CI corre en las dos ramas, así que un fallo se ve en
`dev` antes de que llegue a `main`.

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

> **`npm ci` y Vite no pueden convivir**: `./mvnw` recrea `node_modules`, así que falla si el servidor de Vite está levantado (mantiene abierto `esbuild.exe`). Para lanzar solo los tests de Java sin bajar el frontend:
> ```bash
> ./mvnw test -Dskip.frontend=true
> ```

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

Hay **un solo token** en toda la app: el del CSRF de Spring Security, que viaja en la cookie `XSRF-TOKEN` (legible por JS) y se devuelve en la cabecera `X-XSRF-TOKEN`.

1. **`GET /api/csrf-token`** — solo si la cookie aún no está (`ensureCsrfCookie`). Responde **204** y emite la cookie; **no crea sesión ni guarda nada en servidor**.
2. **`GET /api/projects`** — con la cabecera **`X-XSRF-TOKEN: <token>`** y `credentials: 'include'`. Devuelve `List<RepoDTO>`.
3. **`POST /api/chat`** — mismo token, misma cabecera. Lo valida el CSRF nativo.

Ver "Los dos mecanismos CSRF" más abajo para por qué el GET y el POST se validan en sitios distintos con el mismo token.

### Endpoints

| Método | Ruta                | Auth / Header requerido        | Respuesta |
|--------|---------------------|--------------------------------|-----------|
| GET    | `/api/csrf-token`   | ninguno                        | `204` + `Set-Cookie: XSRF-TOKEN` |
| GET    | `/api/projects`     | `X-XSRF-TOKEN` (filtro propio, contra la cookie) | `RepoDTO[]` |
| POST   | `/api/chat`         | `X-XSRF-TOKEN` (CSRF nativo) + cupos | `text/event-stream` de fragmentos |

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
- **`GitHubService`**: pide los repos del usuario a `api.github.com` (`per_page=100`, `sort=pushed`), filtra forks / repo homónimo / sin descripción y mapea a `RepoDTO`. **Una sola lista cacheada** (`Mono.cache(ttl)`, TTL vía `github.cache-ttl-seconds`) sirve a los dos consumidores: `getAllRepos()` la devuelve entera (la lee el chat) y `getFeaturedRepo(n)` recorta los primeros n (las tarjetas). Timeout 7s. Si GitHub falla, devuelve una **lista fallback hardcodeada** de 5 proyectos (nunca rompe).
  - **El recorte va al final, no en la petición.** Antes `getFeaturedRepo(5)` pedía `per_page=10` y recortaba después de filtrar, así que una racha de forks o de repos sin descripción entre los diez con push más reciente dejaba la portada con 4 tarjetas o menos **y nada lo indicaba**. De paso eran dos entradas de caché para los mismos repos en el mismo orden: dos llamadas a GitHub por cada expiración de TTL.
  - **El filtro de forks es `Boolean.TRUE.equals(...)`** y no `!repo.getFork()`: el campo es un `Boolean`, y si GitHub lo omitiera el NPE lo taparía el `onErrorResume` y la web entera caería a la lista de respaldo **pareciendo correcta**, que es el peor fallo posible.
  - El `log.info("Cache miss")` va en un `doFirst` **antes** del `cache(...)`: así sale en cada fallo de caché real y no una sola vez al construir la cadena.
- **Curación de topics** (`GitHubService.pickTopics`): un repo suele traer 10-16 topics, de los que solo se muestran **3**. Se descartan los genéricos (`NOISE_TOPICS`: backend, full-stack…) y el que repite el lenguaje; se priorizan los conceptuales (`CONCEPT_TOPICS`: arquitectura, seguridad, dominio) con un tope de 2 para **reservar hueco al stack**; y `SYNONYM_GROUPS` evita mostrar dos etiquetas que dicen lo mismo (p. ej. `jwt-authentication` + `security`). El resultado es **determinista**: antes se elegía un topic al azar y cambiaba al expirar la caché.
- **`CsrfValidationFilter`** (`@Order(-85)`): intercepta **solo** `/api/projects`. Si falta la cabecera, falta la cookie, o no coinciden → responde `404`. Compara en **tiempo constante** (`MessageDigest.isEqual`). El frontend, ante error, muestra su propio fallback (estado `error` en `useProjects`).
  - **El orden tiene que ser posterior al de todo filtro que ponga cabeceras.** Este responde **cortando la cadena**, así que lo que no haya corrido antes no llega a registrar su `beforeCommit`. En -100 empataba con el `WebFilterChainProxy` de Spring Security y le ganaba el desempate, así que ese 404 salía **sin una sola cabecera**. Reproducido en producción con `curl` (el 403 de `/api/chat`, que sí pasa por la cadena, traía CSP). Lo que importa es el `Cache-Control`: un 404 sin esa cabecera lo puede cachear por heurística una caché compartida (RFC 9111) y servírselo después a un visitante que **sí** trae token.
  - **-99 no bastaba, y es el error fácil de repetir.** Deja el filtro detrás de Security (-100), así que recupera CSP y compañía, pero **sigue por delante de `CacheControlFilter` (-90)**, que es justo quien pone el `Cache-Control` que motivaba el arreglo. De ahí **-85**. Lo vigila `CsrfFlowIntegrationTest.el404LlevaCacheControlYLasCabecerasDeSeguridad`, que solo puede verse con la cadena entera levantada: ningún test unitario del filtro lo detecta.
- **`TrustedClientIpTransformer`** (bean `forwardedHeaderTransformer`): resuelve la IP del visitante desde el **último** valor de `X-Forwarded-For`, no del primero, e ignora la cabecera estándar `Forwarded`. Ver "La IP del visitante" más abajo — sin esto el cupo por IP del chat no valía nada.
- **`CsrfCookieFilter`** (sin `@Order`, el último): el `Mono<CsrfToken>` que Spring Security deja en el exchange es **perezoso** — la cookie `XSRF-TOKEN` no se escribe hasta que alguien se suscribe, y aquí no hay plantilla de servidor que lo haga. Este filtro se suscribe. Solo en `/api/**`: son las únicas respuestas `no-store`, y emitir la cookie junto a un asset `immutable` dejaría que una caché compartida sirviera **el mismo token a todos los visitantes**. Va sin `@Order` (el último) porque tiene que correr por detrás del `WebFilterChainProxy` de Spring Security (orden `-100`), que es quien pone el atributo.
- **Cookie de sesión** (`SecurityConfig.webSessionIdResolver`): `HttpOnly` + `SameSite=Lax` + `Secure` condicional (`session.cookie.secure`, o `SESSION_COOKIE_SECURE=true` en Railway). Secure no puede ir fijo porque en local se sirve por HTTP y el navegador descartaría la cookie. **Quien crea sesiones ahora es solo `ChatRateLimitFilter`**, y únicamente después de pasar los cupos diario y de IP — así el número de sesiones al día queda acotado por el propio cupo. Ver "El almacén de sesiones" abajo.
- **`SecurityConfig`**: CSRF de Spring **activo** para el POST del chat, con `CookieServerCsrfTokenRepository.withHttpOnlyFalse()` (el token lo tiene que leer el JS del navegador) y `ServerCsrfTokenRequestAttributeHandler` **plano**: el handler por defecto en Spring Security 7 es el XOR (protección BREACH), que enmascara el token por petición y lo espera enmascarado de vuelta — incompatible con un cliente que devuelve el valor tal cual lo lee de la cookie. BREACH no aplica aquí: el token no se incrusta en el HTML comprimido, viaja en un `Set-Cookie`. La cookie `XSRF-TOKEN` sale con `SameSite=Lax` y `Secure` condicional, igual que la de sesión. CSP propia (`script-src 'self'`, `font-src 'self'`, `object-src 'none'`, `frame-ancestors 'none'`, etc.) y `Permissions-Policy` negando cámara, micrófono, ubicación, pagos y USB —la web no pide ninguna, así que declararlo es gratis—, todo `permitAll`. También desactiva el `cache()` por defecto de Spring Security, que ponía `no-store` en **toda** respuesta e impedía cachear los estáticos.
- **`CacheControlFilter`** (`@Order(-90)`): fija `Cache-Control` por ruta en `beforeCommit` (para ganar al manejador de estáticos):
  - `/assets/**` y `/fonts/**` → `public, max-age=31536000, immutable` (Vite pone hash de contenido en el nombre; **sustituir una fuente obliga a renombrarla**).
  - `/img/**`, `/docs/**`, `/favicon.svg`, `/sitemap.xml`, `/robots.txt` → `public, max-age=86400` (nombres estables que sí cambian: avatar, CV, y lo que leen los buscadores — caían en el "resto" y un crawler se los re-descargaba enteros en cada pasada).
  - Resto, incluidos `index.html` y `/api/**` → `no-store`. **`index.html` nunca debe cachearse**: es quien apunta a los assets con hash, y cachearlo impediría que llegara un despliegue nuevo.
- La validación CSRF depende de la **cookie** `XSRF-TOKEN`, no de la sesión. En dev el proxy de Vite la preserva; en el jar es same-origin y funciona directo.

### Los dos mecanismos CSRF (decisión consciente)

Conviven dos, y no es un descuido:

| Endpoint | Quién valida | Por qué |
|---|---|---|
| `GET /api/projects` | `CsrfValidationFilter` propio | El nativo **no puede**: ignora los GET |
| `POST /api/chat` | CSRF nativo de Spring Security | Es exactamente su caso |

**Un solo token para los dos**: la cookie `XSRF-TOKEN` que emite Spring Security, devuelta en `X-XSRF-TOKEN`. El filtro propio no inventa nada, solo aplica el mismo double-submit a un método que el nativo no cubre.

**Por qué el filtro propio en `/api/projects`.** Es un **GET**, y el CSRF de Spring ignora por diseño los métodos seguros (GET, HEAD, OPTIONS, TRACE): configurado, dejaría pasar la petición siempre, y no hay opción para cambiarlo. El default es literalmente incapaz de hacer lo que hace ese filtro.

**Y qué es en realidad.** No es protección CSRF: no hay sesión autenticada ni efecto de lado que un tercero pueda forjar leyendo repos públicos. Es un **portero blando** contra llamadas directas y scraping — obliga a encadenar dos peticiones con cookie. Como barrera es débil a propósito: descartar la cookie solo cuesta una petición más, por eso el chat tiene además cupos de uso. El `404` (en vez de `403`) es para no confirmar siquiera que el endpoint existe.

**Antes iba contra la sesión, y eso era un fallo de disponibilidad.** El token se generaba en `/api/csrf-token` y se guardaba en la `WebSession`, lo que convertía ese endpoint —público, sin cupo y sin necesidad de cookie— en una fábrica de sesiones. Ver "El almacén de sesiones" abajo.

**Por qué el chat sí va por el nativo.** Es un POST con efecto real (gasta cuota del modelo): ahí la semántica CSRF aplica de verdad y el mecanismo estándar la cubre sin código propio. Antes usaba el filtro custom, lo que era reimplementar algo que el framework ya hacía mejor.

**No hay colisión posible** porque ya no hay dos tokens ni dos nombres de cabecera. Ojo si alguna vez se toca: `HttpHeaders` de Spring es case-insensitive, así que `X-CSRF-Token` y `X-CSRF-TOKEN` serían la misma cabecera.

### El almacén de sesiones (por qué el token ya no vive en la sesión)

`InMemoryWebSessionStore` tiene un tope de **10.000 sesiones** y, al llegar, lanza `IllegalStateException` en vez de descartar las viejas. Con el token CSRF guardado en la sesión, `GET /api/csrf-token` creaba una sesión por llamada, sin cupo y sin necesidad de traer cookie: **~10.000 peticiones y la web respondía 500 a todo visitante** durante los 30 minutos que tarda una sesión en caducar. Reproducido con curl en segundos; el chat quedaba inutilizable y los proyectos caían al respaldo del frontend.

El arreglo no es ponerle un cupo: es **no guardar nada**. El token vive en la cookie, `/api/csrf-token` responde 204, y el único sitio que crea sesión es `ChatRateLimitFilter`, ya detrás de los cupos. Hay test de regresión (`CsrfFlowIntegrationTest.pedirElTokenNoCreaSesionEnServidor`) que falla si alguien vuelve a escribir en la sesión desde ahí.

### La IP del visitante (`TrustedClientIpTransformer`)

`server.forward-headers-strategy=framework` hace que Spring saque la IP de `X-Forwarded-For`, y coge el valor **más a la izquierda** (`ForwardedHeaderUtils.parseForwardedFor` → `getLeftMostValue`). Como un proxy **añade** la IP real por la derecha, el valor izquierdo es siempre el que escribió el cliente: mandar `X-Forwarded-For: <lo que sea>` estrenaba un cupo limpio en cada petición y **dejaba inútil el límite por IP**, que es la barrera de la que depende el gasto del chat. La cabecera estándar `Forwarded` daba una segunda vía. Verificado con curl, incluida la forma exacta que produce Railway (`"7.7.7.7, 127.0.0.1"`).

No se puede arreglar en un `WebFilter`: el transformer **borra** las cabeceras `X-Forwarded-*` antes de que corra ninguno. Tampoco por configuración — el stack reactivo no tiene lista de proxies de confianza, y el javadoc de Spring lo dice sin rodeos: *"An application cannot know if forwarded headers were added by a trusted proxy or by a malicious client"*. Así que se sustituye el bean `forwardedHeaderTransformer` por uno que coge el **último** valor, que es el que pone Railway y a cuya derecha el cliente no puede escribir.

**Asume exactamente un proxy delante.** Si se mete otra capa (una CDN sobre Railway), todos los visitantes compartirían cupo: molesto, pero falla **cerrado**, que es como debe fallar un límite de uso. Lo que no se puede es volver a confiar en el valor de la izquierda.

**Y lo dice si pasa.** Esa suposición no se puede comprobar desde fuera —la app no publica en ninguna respuesta qué IP ha resuelto—, así que la comprueba ella: si el último valor no es una dirección pública (loopback, RFC1918, CGNAT `100.64/10`, link-local, `fc00::/7`, o algo que ni siquiera es una IP), deja **un aviso en el log, una sola vez por arranque**. Es una condición de configuración, no un evento por petición. Sin esto el síntoma sería el chat limitado a 15 mensajes/hora **para todo el mundo**, sin nada en ningún sitio que lo explicara. En local no aparece: sin `X-Forwarded-For` el transformer no toca nada.

**Orden de filtros**: el `WebFilterChainProxy` de Spring Security va en `-100`, así que el 403 por CSRF inválido ocurre **antes** de `ChatRateLimitFilter` (`-95`): una petición sin token no consume cupo.

### Secciones / componentes React
- `CustomCursor` — cursor custom con lag (solo mouse fino). El hover va **delegado en `document`** (`mouseover`/`mouseout` + `closest('a, button')`): con un `querySelectorAll` al montar, las tarjetas de proyectos, el panel del chat y el drawer —todo lo que aparece después— no lo activaban nunca.
- `Navbar` — se vuelve sólido tras 40px de scroll; hamburguesa togglea el drawer; incluye `LanguageToggle`.
- `LanguageToggle` — botones ES/EN (banderas SVG inline, no emoji) que llaman a `setLang` del `LanguageContext`.
- `MobileDrawer` — drawer mobile (estado en `App`, cierra con Escape / al navegar). Cerrado lleva **`inert` + `aria-hidden`**: por debajo del breakpoint es `display:flex` también cerrado, y ni `opacity:0` ni `pointer-events:none` sacan nada del orden de tabulación, así que el foco se iba a seis elementos invisibles fuera de pantalla. Incluye una entrada al chat de IA: en móvil el botón flotante es un icono fácil de pasar por alto, así que el menú es la segunda vía de entrada. Mientras el drawer está abierto, `App` esconde el botón flotante (`hidden`) para no duplicar la misma acción.
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

### Cómo se presenta Adrián (posicionamiento)

**Estudiante de DAW que construye backend con Java y Spring**, no un especialista con
experiencia profesional: el ciclo lo empezó en septiembre de 2026 y lo que respalda el
perfil son proyectos propios. Las capacidades técnicas que la web enseña (microservicios,
Kafka, seguridad) se atribuyen a esos proyectos, no a un puesto.

Ese mensaje vive en **cinco sitios que hay que mover a la vez**: `frontend/src/i18n/translations.js`
(hero, sobre mí, contacto, footer, en `es` **y** `en`), `frontend/index.html` (title, meta,
Open Graph, JSON-LD), `src/main/resources/chat/profile.md` (lo que el asistente puede
afirmar), `cv/` (el CV que se descarga) y el `README.md`. Antes estaban desalineados —la
portada vendía "especializado" y el chat decía la verdad—, y esa contradicción la ve
cualquier reclutador que pregunte al chat.

El **README** se quedó fuera la primera vez que se alineó todo lo demás, y siguió meses
diciendo "Portfolio Backend Dev" y hablando de "mi experiencia como desarrollador
backend". Es lo primero que lee quien llega al repositorio desde la web, así que cuenta
tanto como la portada.

### El CV (`cv/`)

Los PDF de `frontend/public/docs/` **se generan** desde `cv/cv-es.html`, `cv/cv-en.html` y
`cv/cv.css` con `pwsh cv/render.ps1` (Chrome headless). No se editan a mano: antes solo
existían los PDF —salidos de un wkhtmltopdf cuyo HTML no estaba en ninguna parte—, así que
añadir una línea obligaba a rehacer el documento.

El diseño está condicionado por el **ATS** que lee el CV antes que nadie: sin
`letter-spacing`, sin tablas, una columna, encabezados con los nombres que un ATS busca
literalmente y fechas `MM/AAAA`. El detalle y cómo comprobarlo, en `cv/README.md`. **Cabe
en una página justo**: al añadir contenido hay que renderizar y contar páginas.

**El script borra el PDF antes de llamar a Chrome y espera a que el tamaño se
estabilice.** Las dos cosas arreglan el mismo agujero: el `Test-Path` final pasaba
siempre que hubiera un PDF de una tirada anterior, así que un fallo de Chrome se saldaba
con "Listo" y se publicaba el CV viejo creyendo haberlo regenerado —y con los errores de
Chrome yendo a `2>$null`, sin rastro—. Al borrarlo primero salió lo que eso tapaba: el
headless nuevo **se desentiende del proceso que escribe el fichero**, así que al volver
de la llamada el PDF puede no estar todavía y la comprobación fallaba aunque Chrome
hubiera acabado bien.

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
- `GitHubServiceTest` — unitario, sin red: construye el `WebClient` con `exchangeFunction(...)` fake para simular respuestas de GitHub. Cubre filtrado (forks / repo homónimo / sin descripción), mapeo a `RepoDTO`, fallback ante error, caché (no repite la llamada HTTP), el header `Authorization: Bearer` condicionado a que haya token, y la curación de topics (genéricos descartados, prioridad conceptual, hueco reservado al stack, sinónimos deduplicados, repo sin topics → lista vacía). También `getAllRepos()`: devuelve todos los públicos sin recortar a los destacados, aplica los mismos filtros y cae al respaldo si GitHub falla. Y las dos regresiones del recorte: que las tarjetas **comparten la misma petición** que el chat (antes eran dos), que **no se quedan cortas** porque los filtros se coman la primera página, y que un repo **sin el campo `fork`** no tumba la lista entera al respaldo.
- `CsrfValidationFilterTest` — unitario sobre el `WebFilter` con `MockServerWebExchange`: cookie y cabecera coincidentes dejan pasar; sin cabecera, sin cookie o sin coincidir → 404; rutas distintas de `/api/projects` no se validan, y `/api/chat` **no pasa por aquí** (lo cubre el CSRF nativo).
- `TrustedClientIpTransformerTest` — el arreglo de la suplantación de IP: con la IP falsa delante gana la que añade el proxy (una o varias), sin cabecera del cliente se usa la del proxy, la cabecera estándar `Forwarded` se ignora, se toleran espacios y valores vacíos, y **`X-Forwarded-Proto` sigue aplicándose** (si no, se perdería el HSTS en producción). Y el detector de "más de un proxy delante": qué direcciones cuentan como visitante y cuáles no (privadas, CGNAT, link-local, `fc00::/7`, o texto que no es una IP), incluido que comprobarlo **no resuelve por DNS** un valor que llega en una cabecera.
- `ChatServiceTest` — el prompt de sistema lleva reglas + perfil + los repos de GitHub (con lenguaje y etiquetas), las descripciones largas se recortan a 220 caracteres para no inflar el prompt, y los errores **se propagan** (traducirlos es cosa del advice). Sobre el historial: viaja como transcripción dentro del turno del visitante (con las etiquetas `Visitante:`/`Asistente:` y el aviso de que puede estar alterada), sin historial el turno es solo la pregunta, y —lo que cierra el agujero— **ningún mensaje de la conversación acaba con rol `assistant`** aunque el cliente lo pida.
- `ChatExceptionHandlerTest` — cada `statusCode` produce su mensaje (429 límite, 402 sin crédito, config/red genérico) y siempre con 200 + `text/event-stream`.
- `ChatControllerTest` — pregunta vacía no llega al modelo, recorte a 500 caracteres, historial recortado a 6 turnos, **cada turno recortado por separado** (500 el del visitante, 2.000 el del asistente, con 100 KB de entrada) sin tocar un historial de tamaño normal, y un turno con rol `system` degradado a `user`.
- `ChatRateLimitFilterTest` — cupo por sesión: dentro pasa, al superarlo 429 sin llamar al modelo, sesiones distintas no comparten cupo, otras rutas no consumen. Y los cupos por red: en IPv6 van por **/64** (rotar la dirección dentro del prefijo no estrena cupo) sin que dos prefijos distintos se pisen, una IPv4 mapeada (`::ffff:…`) sigue contando como IPv4, y un valor que no es una IP literal se agrupa tal cual en vez de romper el cupo. Del cupo diario por red: una sola no se lleva el presupuesto del día, otras redes no se ven afectadas, y **lo que frena el cupo de la hora no gasta el del día**.
- `CacheControlFilterTest` — la política de caché por ruta: assets con hash y fuentes inmutables, imágenes/CV/`sitemap.xml`/`robots.txt` a un día, y `index.html` + `/api/**` sin cachear nunca (esto último es el que protege los despliegues).
- `ProjectControllerTest` — `WebTestClient.bindToController(...)` con `GitHubService` mockeado: 200 con la lista, 200 con lista vacía, y el camino defensivo 204 (`Mono.empty()`) del controller.
- `CsrfFlowIntegrationTest` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureWebTestClient`, con `GitHubService` reemplazado por `@MockitoBean`: valida el flujo real csrf-token → cookie + cabecera → `/api/projects`, con sus caminos 404 (sin cabecera, sin cookie, o sin coincidir). Incluye la **regresión que importa**: `pedirElTokenNoCreaSesionEnServidor`, que falla si `/api/csrf-token` vuelve a emitir cookie `SESSION`.
- `ChatCsrfIntegrationTest` — el CSRF nativo sobre `POST /api/chat`, que al vivir dentro de la cadena de Spring Security solo se puede comprobar con el contexto levantado: una respuesta de `/api/**` emite la cookie, un estático **no** (el caso que protege de las cachés compartidas), y el POST pasa solo si cookie y cabecera coinciden — sin cabecera o con otra, 403 sin llegar al modelo.

### Frontend (Vitest + @testing-library/react + jsdom)
```bash
cd frontend
npm test          # una pasada (CI)
npm run test:watch
npm run lint      # ESLint (flat config + react-hooks)
```
- `api/client.test.js` — `getProjects` contra `fetch` mockeado: cabecera/credentials correctos, error si la respuesta no es `ok`, `[]` en `204`. Y `ensureCsrfCookie`: devuelve la cookie sin tocar la red si ya está, provoca una respuesta del backend si no, y falla claro si tras eso sigue sin haberla.
- `hooks/useProjects.test.js` — estados `loading` → `success`/`error` mockeando `api/client.js` (`ensureCsrfCookie` + `getProjects`).
- `hooks/useRevealOnScroll.test.jsx` — `IntersectionObserver` mockeado: observa los `.rv` al montar, añade `.on` al intersectar, `disconnect()` al desmontar.
- `i18n/LanguageContext.test.jsx` — detección de idioma (`localStorage` > navegador > default), `setLang` (persistencia, `<html lang>`, idiomas no soportados), error al usar `useLanguage` fuera del provider.
- `components/MobileDrawer.test.jsx` — el menú móvil ofrece el chat (traducido) y avisa a `App` al pulsarlo. Y la regresión de accesibilidad: **cerrado lleva `inert` + `aria-hidden`**, abierto no.
- `components/Chat.test.jsx` — el panel lo controla `App`: cerrado solo se ve el lanzador, `open` lo abre desde fuera y `hidden` esconde el lanzador con el menú abierto. Y que **al cerrarse corta el stream** en curso.
- `components/RichText.test.jsx`, `LanguageToggle.test.jsx`, `Contact.test.jsx`, `Footer.test.jsx` — comportamiento observable: negritas → `<strong>`, botón de idioma activo/click, CV descargable por idioma, año dinámico.
- `api/streamChat.test.js` — el parser SSE: reconstruye el texto, aguanta que un evento llegue troceado entre lecturas, une varias líneas `data:` de un mismo evento, manda el token en `X-XSRF-TOKEN`, y distingue 429 del resto.
- `hooks/useChat.test.js` — turno de usuario + turno del asistente rellenándose con el stream, preguntas vacías ignoradas, token CSRF reutilizado, y mensajes de límite/error sin romper la UI. Más el aborto: `cancel()` corta el stream **en vuelo** y no deja mensaje de error en el turno a medias.
- `components/ProjectCard.test.jsx` — título legible (y respeto de mayúsculas existentes), chips de lenguaje y topics, ausencia de chips si el repo no trae topics, "actualizado hace X" localizado (con `vi.setSystemTime`) y omitido si no hay `pushed_at`, enlace y descripción de respaldo.

No hay tests de `CustomCursor` (loop de `requestAnimationFrame` puramente imperativo) ni de componentes de solo layout (`Hero`, `About`, `Navbar`, `MobileDrawer`, `GithubCard`, `Projects`) — bajo valor relativo al esfuerzo de mockear DOM/IntersectionObserver para lo que son, en esencia, vistas sin lógica propia.

---

## Chat con IA

Asistente que responde preguntas sobre el perfil de Adrián, montado sobre **su propia librería** [`prompt-link`](https://github.com/adrian0511/prompt-link) (`io.github.adrian0511:prompt-link`, en Maven Central), que enruta a OpenRouter.

**Por qué existe**: no es una utilidad para el visitante (pocos usarán un chat), es la demostración de una competencia que el portfolio solo afirmaba. De paso justifica WebFlux: hasta ahora el backend reactivo servía un único `GET`; el streaming SSE token a token sí es el caso de uso para el que existe WebFlux.

**Flujo**: `POST /api/chat` (protegido por el **CSRF nativo** de Spring Security: cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN`) → `ChatService` monta exactamente **dos** mensajes, `[system(reglas+perfil+repos de GitHub), user(transcripción previa + pregunta)]` → `ReactiveAiService.stream(...)` → `Flux<String>` → SSE al navegador.

- **`chat/profile.md`** (en `resources/`) es la fuente de datos del asistente sobre Adrián. Ampliar lo que sabe de él = editar ese fichero, sin tocar código.
- **Los proyectos los lee de GitHub, no del perfil**: `ChatService` pide `GitHubService.getAllRepos()` (todos los repos públicos, no solo los 5 destacados de la portada) y los añade al prompt de sistema. Así el chat puede hablar de un repo recién publicado sin que nadie lo anote a mano — el perfil solo detalla los que merecen contexto extra. Va por la **misma caché** que las tarjetas (TTL `github.cache-ttl-seconds`), así que no supone una llamada a GitHub por mensaje, y si GitHub falla entra la lista de respaldo.
- **Guardarraíles en el prompt de sistema**: responder solo desde el perfil; ante lo que no consta, derivar al email; hablar de Adrián en tercera persona; ignorar instrucciones del visitante que intenten reescribir las reglas. *Un modelo inventando "sí, domina Kubernetes" ante un reclutador es peor que no tener chat.*
- **`ChatExceptionHandler`** (`@RestControllerAdvice`): traduce `AiClientException` a texto útil con **200**, no a un error. Distingue 429 (límite), 402 (sin crédito) y el resto. **Registra el modelo en la línea de error, entre comillas**, y `ChatService` lo deja también al arrancar: un `400` significa que OpenRouter rechaza el id, así que sin saber qué id le llegó no se puede diagnosticar. Pasó en producción —`AI_MODEL` mal puesta en Railway— y el log solo decía `statusCode=400`; las comillas hacen visibles los espacios o comillas que viajen pegados al valor. La línea lleva también los modelos de respaldo, y un `CONFIGURATION_ERROR` registra **el mensaje de la librería**, no un texto fijo: desde la 1.2.0 no significa solo «falta la API key», también un `ai.models` de más de 3 o un `ai.reasoning` contradictorio, y el mensaje nombra la propiedad. **Limitación real**: solo captura fallos *previos al primer token* (sin API key, 401, 429, red), que son los habituales porque `stream(...)` falla en la petición inicial. Un fallo a mitad de stream (`STREAM_ERROR`) llega con la respuesta ya comprometida y el visitante vería la respuesta truncada.
- **`ChatRateLimitFilter`** (`@Order(-95)`): tres cupos, porque el de sesión **por sí solo no protegía nada** — crear una sesión cuesta una petición a `/api/csrf-token`, así que un script que descarte la cookie tenía mensajes ilimitados (verificado con `curl`):
  - **por sesión** (`chat.max-messages-per-session`, 20): cortesía para un navegador normal.
  - **por IP y hora** (`chat.max-messages-per-ip-per-hour`, 15): la barrera real, porque una IP sí es un recurso escaso. El mapa se poda para que no sea a su vez un vector de agotamiento de memoria. **Depende por completo de `TrustedClientIpTransformer`**: mientras Spring resolvía la IP desde el valor izquierdo de `X-Forwarded-For`, este cupo se saltaba con una cabecera y no protegía nada.
    - **En IPv6 el cupo va por `/64`, no por dirección.** Una IPv6 suelta no es escasa: a un visitante doméstico se le asigna un `/64` entero, así que podía estrenar dirección en cada petición. Comprobado con curl contra el jar: 20 peticiones rotando `2001:db8:1:1::N` pasaban enteras con el cupo en 15, y lo único que las frenaba era el tope diario global. El `/64` es el bloque más pequeño que se reparte de una pieza. Se agrupa con `InetAddress.ofLiteral` (y no `getByName`, que resolvería por DNS un valor que viene de una cabecera); una IPv4 mapeada `::ffff:…` la devuelve como `Inet4Address`, así que no acaba toda junta en el mismo cupo.
  - **por red y día** (`chat.max-messages-per-ip-per-day`, 20): que una sola red no se lleve el presupuesto del día. El de la hora no basta: a 15/hora, una máquina vacía en una tarde la cuota diaria del modelo (**50 peticiones** en el tier gratuito de OpenRouter) y deja el chat mudo para cualquier visitante. Con este, hacen falta varias redes distintas. Un visitante de verdad no manda 20 mensajes en un día.
  - **global diario** (`chat.max-messages-per-day`, 150): última línea de defensa; acota el gasto aunque el atacante tenga muchas IPs. **Ojo: hoy no llega a aplicarse** — con el tier gratuito (50/día) el 429 lo da OpenRouter mucho antes, y el `ChatExceptionHandler` lo traduce. Este tope solo empezaría a morder si algún día se compran créditos (1.000/día).

  El orden importa: si una petición ya la para el cupo de la hora, **no gasta el del día**. Rechazarla y cobrársela a la red dos veces dejaría que una racha corta agotara su cuota diaria entera.
- **Nada de filtrar por `Origin`/`Referer`**: se comprobó en un navegador real que **no envía `Origin`** en este POST, y `Referrer-Policy: no-referrer` (de Spring Security) impide el `Referer`. Exigir cualquiera de las dos habría bloqueado a los visitantes de verdad.
- **Topes de entrada en el controller**: pregunta a 500 caracteres, historial a los 6 últimos turnos, y **cada turno recortado por separado** — 500 los del visitante (un turno suyo es una pregunta pasada), 2.000 los del asistente (los generó el modelo con `ai.max-tokens=600`, no dan para más). Sin ese recorte por turno los otros dos topes no valían nada: el techo real era el del cuerpo HTTP (`spring.codec.max-in-memory-size`, 256 KB), y se comprobó con curl que **200 KB de historial llegaban enteros al prompt**, ~500 veces lo que el tope de la pregunta aparentaba permitir. Los turnos con rol `system` se degradan a `user`.
- **El historial no se reenvía como turnos de la conversación**: `ChatService` lo mete como **transcripción etiquetada dentro del mensaje del visitante** (`CONVERSACIÓN PREVIA`), con la regla 9 diciendo que lo aporta su navegador y puede estar falseado. El motivo: el cliente elige el rol de cada turno, así que podía **fabricar respuestas del propio asistente** ("Adrián tiene 8 años con Kubernetes") y luego preguntar por ellas; un modelo pondera sus propios turnos previos mucho más que lo que le pida el usuario, así que era el camino corto para sacarle justo lo que las reglas intentan evitar — y la captura de pantalla que un reclutador no debería ver nunca. Ahora **ningún mensaje de la conversación lleva rol `assistant`**, y el contexto del hilo se conserva igual. Tampoco va en el prompt de sistema: ahí el texto del visitante tendría aún más autoridad; el sitio correcto es el turno del usuario.
- **Sin API key el chat no rompe**: responde con el mensaje de respaldo derivando al email, igual que los proyectos tienen su lista de respaldo.

**Dónde va la API key**:
- **Local**: `config/application.properties` (en la raíz del proyecto, **git-ignorado**). Spring Boot lee `./config/` automáticamente y sus valores ganan a los de `src/main/resources`, sin perfiles ni flags ni dependencias. Es el sustituto nativo de un `.env`, que Spring **no** lee de serie.
  - *No buscar librerías de `.env`*: `spring-dotenv` no sirve aquí — su última versión es de mayo de 2023 y no funciona con Spring Boot 4. La ventaja de `./config/` es justamente que forma parte de la resolución de configuración del propio Spring Boot, así que no se rompe al subir de versión.
- **Railway**: variable de entorno `OPENROUTER_API_KEY`.
- **Nunca** en `frontend/.env`: Vite inlinea las variables `VITE_*` en el bundle público y la clave quedaría a la vista de cualquiera.
- Hay que **reiniciar** el backend tras ponerla: Spring la lee al arrancar.

**Configuración** (`ai.*` las lee la librería):
- `ai.api-key=${OPENROUTER_API_KEY:}` — sin ella, modo respaldo.
- `ai.model=${AI_MODEL:nvidia/nemotron-3-ultra-550b-a55b:free}` — el tier gratuito de OpenRouter son **20 req/min y 50 req/día** (1.000/día si alguna vez se compran $10 en créditos). Al agotarse entra el mensaje de respaldo. Se eligió Nemotron Ultra probando todos los modelos gratuitos con una pregunta sobre algo que **no** consta en el perfil («¿sabe Kubernetes?»): fue el que mejor se ciñó a las reglas. Los de código (`cohere/north-mini-code`) adornan, y otros (`thinkingmachines/inkling`) solo se pueden usar desde agentes registrados.
- `ai.models=${AI_MODELS:google/gemma-4-31b-it:free,openrouter/free}` — **modelos de respaldo** (prompt-link 1.2.0, máximo 3): si el principal cae, OpenRouter responde con el siguiente **en la misma petición**, sin reintento propio, así que vale con streaming. Existe porque con un solo modelo **el chat entero caía con él**: `gemma-4-31b-it:free` estuvo **días** devolviendo 502/504 —Google limitaba el grupo gratuito compartido (`429 temporarily rate-limited upstream`)— y todo visitante veía el mensaje de respaldo. `gemma-4-26b-a4b-it:free` no sirve de respaldo de Gemma: va por el mismo proveedor y cae a la vez. `openrouter/free` va el último porque es un enrutador que siempre encuentra algún modelo libre (a cambio, no se sabe cuál). Cambiar `AI_MODEL`/`AI_MODELS` en Railway **no necesita despliegue**, y los valores de este fichero solo se aplican si esas variables no están puestas: conviene mirar cuál manda antes de editar nada.
- `ai.reasoning.effort=${AI_REASONING_EFFORT:low}` — casi todos los modelos gratuitos razonan antes de escribir, y **ese razonamiento sale del mismo `max-tokens=600`** que la respuesta. Medido con el prompt real sobre Nemotron Ultra: sin límite, ~10 s hasta el primer token; con `low`, ~2 s y la misma fidelidad. **No se apaga (`enabled=false`)** aunque sea más rápido (0,5 s): sin razonar, el modelo se inventó «Spring Cloud» y «API Gateway», que no estaban en el prompt, que es justo lo que los guardarraíles intentan evitar. Si un modelo aun así se gasta los tokens sin escribir, la 1.2.0 lo convierte en `INVALID_RESPONSE` y sale el mensaje de respaldo en vez de una burbuja vacía.
- `ai.read-timeout=25s` — seguro **porque hay streaming**: el primer token llega rápido. En una llamada no-streaming OpenRouter no envía nada hasta terminar de generar, y 25s mataría respuestas largas. Es un timeout de **inactividad**: los fragmentos de razonamiento y los keep-alive de OpenRouter cuentan como actividad, así que un modelo que tarde en escribir no se corta mientras siga mandando algo. La latencia del tier gratuito **varía mucho** con el mismo prompt (medido: de 3,6 s a 26 s hasta el primer token): es la cola del proveedor, no la configuración.

**`PKIX path building failed` probando el chat en local**: es el antivirus (Kaspersky) del equipo de desarrollo, que intercepta parte de las conexiones HTTPS y las refirma con su propia raíz. Windows confía en ella; el almacén de certificados de Java, no. Falla **a ratos** (solo en las conexiones que intercepta), lo que lo hace parecer un fallo intermitente del código. Se comprueba con `openssl s_client -connect openrouter.ai:443`: si el emisor es `Kaspersky Anti-Virus Personal Root Certificate`, es eso. **No afecta a producción**: la cadena real (Google Trust Services) está en el JDK y en Railway no hay antivirus de por medio.

**Cerrar el panel aborta el stream**: `useChat` crea un `AbortController` por envío y
expone `cancel()`, que llama la limpieza del efecto de `Chat` —el momento exacto en que
el panel se cierra— y también el desmontaje. Sin eso el `fetch` seguía vivo escribiendo
en un estado que ya no se pinta. Un `AbortError` **no se pinta**: abortar es una decisión
del visitante, y mostrarlo dejaría un "no puedo responder" pegado al final del turno a
medias. El panel lleva además `aria-modal` y devuelve el foco al lanzador al cerrarse (si
no, se queda en el `<body>` y la siguiente tabulación empieza desde arriba de la página).

**En móvil**: el estado abierto/cerrado del chat vive en `App` (no dentro de
`Chat`) porque lo abren dos sitios: el botón flotante y el menú hamburguesa. El
panel pasa a ocupar el ancho de la pantalla por debajo de 520px y se mide en
`dvh`, no en `vh`, para que la barra del navegador no deje el campo de escribir
fuera de la parte visible.

**Sin RAG a propósito**: el perfil son dos páginas y cabe entero en el prompt de sistema. Montar embeddings para eso sería sobreingeniería.

---

## Rendimiento

Decisiones tomadas y por qué (medido con Playwright contra el jar de producción):

- **Caché de estáticos** (`CacheControlFilter`): antes todo salía con `no-store` por el default de Spring Security, así que **cada visita recurrente re-descargaba ~306 KB**. Ahora la 2ª visita solo pide `index.html` y la API (~2,5 KB); el resto sale de caché.
- **Fuentes autoalojadas**: elimina 2 handshakes DNS+TLS a `fonts.googleapis.com`/`fonts.gstatic.com` y permite servirlas con caché `immutable` propia. Son **variables**: un fichero por familia+subset cubre todos los pesos (declarados como rango, `font-weight: 400 700`). Con `unicode-range`, en es/en solo se descarga el subset `latin`.
- **`Avatar.jpg` (50 KB) sustituye a `Avatar.png` (189 KB)**: la foto la sirve el `<picture>` como webp (14 KB) a casi todos los navegadores; el JPEG es el respaldo y el `og:image`. PNG es mal formato para un retrato.
- **Compresión**: no se activa `server.compression` porque **Railway ya comprime en su proxy** (verificado: `Content-Encoding: gzip` con `Server: railway-hikari`). Activarla en el origen no aportaría nada al usuario; haría falta si se cambia de hosting.
- **No se hizo code splitting** (es una sola página) ni se sustituyó React por Preact (~30 KB gzip de ahorro, pero riesgo alto para el valor).

### Desbordamiento lateral y viewport móvil

`body { overflow-x: hidden }` **no basta**: el `overflow-x` de `body` se propaga
al viewport, así que `body` deja de recortar y el navegador móvil ensancha el
*layout viewport* hasta abarcar lo que sobresale. Con los glows decorativos
(`.cglow`, 500px) el layout pasaba a 445px en una pantalla de 390: todo lo
fijado a la derecha —el botón del chat— quedaba fuera de la pantalla, sin forma
de pulsarlo. Se arregla con `html { overflow-x: clip }` y conteniendo el glow en
su sección (`#contact { overflow: hidden }`, como ya hacía `#hero`).

Se detecta midiendo `document.documentElement.scrollWidth` contra
`visualViewport.width` en un navegador con emulación móvil real: en un viewport
de escritorio estrecho el fallo **no aparece**, solo sale un scroll horizontal.

## Convenciones

- Paquete raíz: `com.adrian.portfolio`.
- Backend reactivo: usar `Mono`/`Flux`, **no** bloquear.
- DTOs con Lombok (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`).
- Frontend: React funcional con hooks; JavaScript (no TS); CSS global por ahora.
- Español en textos de UI y (parcialmente) comentarios.
- **Comentarios: solo los necesarios, y escritos como los escribiría un senior.** La regla es una: un comentario existe para decir **por qué**, nunca **qué**. Si repite lo que ya dicen el nombre del método o la línea de debajo, sobra — y un comentario de más no es neutral: envejece, miente antes que el código y le quita peso a los que sí importan.

  **Se comenta:**
  - la restricción externa que obliga a escribirlo así (un default del framework, un proxy delante, un límite del proveedor);
  - la alternativa evidente que se descartó y por qué (`ofLiteral` y no `getByName`, el último valor de `X-Forwarded-For` y no el primero);
  - el orden o la posición que no se puede cambiar sin romper algo (`@Order`, `beforeCommit`, un cupo que se comprueba antes que otro);
  - la invariante que costó un incidente y que alguien podría deshacer de buena fe;
  - en los tests, **qué regresión vigila** el caso, que es lo que un nombre de método no llega a contar.

  **No se comenta:** lo que hace la línea siguiente; la arqueología del "antes esto era así" cuando ya no cambia ninguna decisión; el relato largo de un incidente —eso vive en este fichero, el código se queda con la conclusión—; ni adornos tipo "fix definitivo".

  **Forma:** en el javadoc, primero la frase que lo resume y después el porqué; frases cortas, sin épica. Mejor renombrar algo que explicarlo con un comentario.

## Notas / deuda técnica conocida
- **`spring.codec.max-in-memory-size` sigue en su valor por defecto (256 KB)**, que es el techo del cuerpo de `/api/chat`. No se baja a propósito: esa propiedad la aplica Spring Boot también al `WebClient`, y la lista completa de repos de GitHub (`per_page=100`) puede acercarse a ese tamaño. Lo que acota el coste del prompt son los topes por turno del controller, no esta propiedad.
- **Java 25 requerido para buildear** (`java.version=25` en el `pom.xml`, igual que el Dockerfile). Antes las propiedades decían 21 y el plugin forzaba 25, lo que hacía creer que bastaba un JDK 21.
- Links del drawer y del `#contact` ya apuntan a los perfiles reales (`github.com/adrian0511`, `linkedin.com/in/adrdev`).
- Estilos inline ya migrados a CSS en `ProjectCard` (`.pc-link`), `GithubCard` (`.gh-*`) y `Hero` (`.photo-stack`). Quedan algunos sueltos en `Projects` (mensaje de error).
- CSS global pendiente de pasar a CSS Modules de forma incremental.
- **CI en GitHub Actions** (`.github/workflows/ci.yml`): en cada push/PR a `main` o `dev` corre tests de backend, tests + build de frontend, y por último el `mvnw package` completo (el mismo que ejecuta Railway al desplegar). Declara `permissions: contents: read`: los jobs solo leen el repo, y sin ese bloque el `GITHUB_TOKEN` hereda el permiso por defecto del repositorio, que puede ser de escritura.
- **Dependabot: dos cosas distintas, y conviene no confundirlas.**
  - **Las subidas de versión** las configura `.github/dependabot.yml` (maven y npm semanales, actions mensual), con tres excepciones anotadas: **no se aceptan subidas de línea de Spring Boot** (mayor/menor), solo parches de la 4.0.x —ver la nota de Spring Cloud más abajo—, y **ni los majors de `react`/`react-dom`/`vitest`**, que son decisión tomada y no pendiente.
  - **Los avisos de CVE** son el interruptor *Dependabot alerts* de `Settings → Code security`, que **no** se configura desde ese fichero. Estuvo apagado, así que durante un tiempo una dependencia vulnerable solo aparecía si alguien lanzaba `npm audit` o miraba el árbol de Maven a mano — que es literalmente como salieron los cuatro avisos de BouncyCastle y los cinco de Jackson. Ya está activado (`GET /repos/{owner}/{repo}/vulnerability-alerts` responde 204; con 404 está apagado).
  - **Las *security updates*** (los PR automáticos ante un CVE) se dejan **apagadas a propósito**: su lock lo escribe el npm de Dependabot, y eso no se puede mergear sin regenerarlo con el npm del build.
- **Dos exclusiones en el `pom.xml`, las dos por lo mismo**: dependencias que `prompt-link` arrastra vía Spring Cloud OpenFeign, que aquí no se usan, y que viajaban dentro del jar únicamente para que cualquier escáner de la imagen las marcara.
  - **`commons-fileupload`**: llega por `feign-form-spring` y solo sirve para subir ficheros multipart por Feign (aquí las llamadas a OpenRouter son JSON). La rama 1.x está sin mantenimiento y arrastra CVE-2025-48976. Comprobado tras excluirla: el chat responde entero, sin `NoClassDefFoundError`.
  - **`bcprov-jdk18on`** (BouncyCastle 1.81): llega por `spring-cloud-starter` y lo único que lo usa es el descifrado de configuración del bootstrap de Spring Cloud, que aquí no se activa. Arrastra cuatro avisos, dos **críticos**: reuso de keystream en GOST 28147 CTR (GHSA-574f-3g2m-x479) y bypass de Name Constraints (GHSA-9pwp-9qqc-pr26). Comprobado: la suite entera pasa sin él —los tres tests que levantan contexto incluidos— y desaparece del árbol.
- **Jackson va fijado por delante del parent** (`jackson-bom.version` 3.1.7 y `jackson-2-bom.version` 2.21.7): Boot 4.0.8 trae 3.1.5 y 2.21.5, con cinco avisos cada uno (tres HIGH), y no van a llegar por parche porque 4.0.8 es la última 4.0.x. **Son dos propiedades porque en el jar viajan dos Jackson**: el 3 que usa Boot y el 2 que arrastra `prompt-link`; mientras `prompt-link` no migre, hay que parchear los dos.
  - **Ojo el día que se pueda subir de línea**: estas propiedades *fijan* la versión, así que si un parent futuro trajera un Jackson más nuevo, estas líneas lo **frenarían**. Al subir de Boot hay que comprobar cuál de las dos versiones es mayor y, si gana la del parent, borrarlas.
- **Arranque limpio en producción**: se excluye `ReactiveUserDetailsServiceAutoConfiguration` (la app no autentica a nadie, así que el usuario en memoria de Spring Security y su password aleatoria solo ensuciaban el log) y el `ENTRYPOINT` del Dockerfile pasa `--enable-native-access=ALL-UNNAMED`, que desde Java 24 hace falta para que Netty no avise en cada arranque.
- **Mockito se pasa como `-javaagent`** (surefire + `dependency:properties` en el `pom.xml`): auto-adjuntarse dejará de funcionar en JDKs futuros y avisaba en cada build.
- **`mvnw` tiene que estar en el índice de git como `100755`**: creado desde Windows entró como `100644` y el runner de Linux respondía `Permission denied` (el paso fallaba en 0 s, sin llegar a compilar). Si se vuelve a añadir el wrapper: `git update-index --chmod=+x mvnw`.
- **No se puede subir a Spring Boot 4.1.x**: `prompt-link` arrastra Spring Cloud OpenFeign, y el verificador de compatibilidad de Spring Cloud 2025.1.x aborta el arranque con *"Spring Boot [4.1.1] is not compatible with this Spring Cloud release train — change to [4.0.x]"*. Comprobado: con 4.1.1 fallan los 4 tests que levantan contexto (los unitarios pasan, porque no arrancan Spring). Para subir habría que actualizar antes `prompt-link` a un release train de Spring Cloud compatible con Boot 4.1. **4.0.8 es la última versión usable.**
- **`npm audit` está en 0.** Cinco de los siete avisos que había (`baseline-browser-mapping`, `brace-expansion`, `browserslist`, `source-map-js`, `undici`) se cerraron con el lock, sin tocar `package.json`. Los otros dos eran la cadena `esbuild` ≤0.24.2 vía `vite` 5 y los cerró **Vite 8**, que va sobre rolldown: `esbuild` desaparece del árbol entero en vez de parchearse, y con él el `vite` 8 anidado que traía `vitest` (su peer ya admitía `^8`, así que ahora deduplica).
  - **Lo que hay que vigilar al tocar Vite es el plugin SEO de `vite.config.js`**, que usa dos hooks de la API de Rollup: `generateBundle`/`emitFile` para el `sitemap.xml` y `transformIndexHtml` para `%BUILD_TIME%`. Si uno dejara de ejecutarse, el build **seguiría saliendo**: simplemente no habría sitemap, o el HTML publicado llevaría `%BUILD_TIME%` literal en `og:updated_time` y en `dateModified`. Al subir de Vite hay que mirar el `dist/`, no solo que el build acabe.
- **El `package-lock.json` hay que regenerarlo con el npm del build, no con el del sistema.** El build y el CI usan el npm que viene con el Node fijado (`nodeVersion` del pom = `node-version` del CI → `target/node/npm.cmd`). Escribir el lock con un npm más nuevo lo rompe de una forma que no se ve en local: npm 11 da por no-necesario el `esbuild` anidado del Vite 8 de `vitest` —ahí es un peer *opcional*— y lo omite, pero npm 10 lo considera obligatorio y `npm ci` aborta con `Missing: esbuild@0.28.2 from lock file`. Pasó: `npm audit fix` con el npm del sistema dejó el `mvnw package` sin pasar del frontend. Para tocar dependencias:
  ```bash
  ./mvnw -B com.github.eirslett:frontend-maven-plugin:1.15.1:install-node-and-npm       -DnodeVersion=<el del pom> -DinstallDirectory=target
  cd frontend && ../target/node/npm.cmd install     # o audit fix / update
  ```
  Y comprobarlo con `./mvnw clean package`, no solo con `npm test`: el fallo estaba justo en el paso que `npm test` no ejecuta.
- **`nodeVersion` (pom) y `node-version` (ci.yml) tienen que moverse juntos**, y por encima de lo que pidan los `engines` de las devDependencies: `jsdom` y `undici` subieron su mínimo a 22.19/22.22 y con el 22.18.0 fijado cada instalación escupía avisos `EBADENGINE`.
- **ESLint configurado** (`frontend/eslint.config.js`, flat config con react-hooks): `npm run lint`.
- **`pushed_at` no es la fecha del último commit**: GitHub lo actualiza con cualquier push a *cualquier* rama (ramas de Dependabot, borrado de ramas…), así que el "Actualizado hace X" de las tarjetas puede indicar actividad que no es del autor. La fecha real sería `GET /repos/{owner}/{repo}/commits?per_page=1` (una llamada extra por repo). Se optó por mantener `pushed_at` por simplicidad.
- `hreflang` (es / en / x-default) declarado en `index.html`. Las tres alternativas apuntan a la misma URL porque el idioma se cambia en cliente.
