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
├── .github/workflows/ci.yml    # tests de backend y frontend + package en cada push/PR
├── Dockerfile                  # build multi-stage (JDK -> JRE), usuario no-root
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
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java           # WebFlux security (CSRF nativo, CSP, permitAll)
│   │   │       │   └── TrustedClientIpTransformer.java  # IP real del visitante (X-Forwarded-For)
│   │   │       └── filter/
│   │   │           ├── CsrfValidationFilter.java     # valida X-CSRF-Token en GET /api/projects
│   │   │           └── CsrfCookieFilter.java         # fuerza la cookie XSRF-TOKEN en /api/**
│   │   └── resources/
│   │       └── application.properties           # (static/ ya NO existe: lo genera el build de React)
│   └── test/java/com/adrian/portfolio/
│       ├── PortfolioApplicationTests.java        # context load test
│       ├── CsrfFlowIntegrationTest.java          # flujo csrf-token -> projects de punta a punta
│       ├── ChatCsrfIntegrationTest.java          # CSRF nativo sobre POST /api/chat
│       ├── controller/ProjectControllerTest.java
│       ├── security/config/TrustedClientIpTransformerTest.java
│       ├── security/filter/CsrfValidationFilterTest.java
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
- **`GitHubService`**: pide los repos del usuario a `api.github.com`, filtra forks / repo homónimo / sin descripción, toma los primeros N (5), mapea a `RepoDTO`. `getAllRepos()` devuelve esa misma lista **sin recortar** (la consume el chat), cacheada aparte con el mismo TTL. Cachea la respuesta en memoria (`Mono.cache(ttl)`, TTL vía `github.cache-ttl-seconds`) para no repetir la llamada a GitHub en cada visita. Timeout 7s. Si GitHub falla, devuelve una **lista fallback hardcodeada** de 5 proyectos (nunca rompe).
- **Curación de topics** (`GitHubService.pickTopics`): un repo suele traer 10-16 topics, de los que solo se muestran **3**. Se descartan los genéricos (`NOISE_TOPICS`: backend, full-stack…) y el que repite el lenguaje; se priorizan los conceptuales (`CONCEPT_TOPICS`: arquitectura, seguridad, dominio) con un tope de 2 para **reservar hueco al stack**; y `SYNONYM_GROUPS` evita mostrar dos etiquetas que dicen lo mismo (p. ej. `jwt-authentication` + `security`). El resultado es **determinista**: antes se elegía un topic al azar y cambiaba al expirar la caché.
- **`CsrfValidationFilter`** (`@Order(-100)`): intercepta **solo** `/api/projects`. Si falta la cabecera, falta la cookie, o no coinciden → responde `404`. Compara en **tiempo constante** (`MessageDigest.isEqual`). El frontend, ante error, muestra su propio fallback (estado `error` en `useProjects`).
- **`TrustedClientIpTransformer`** (bean `forwardedHeaderTransformer`): resuelve la IP del visitante desde el **último** valor de `X-Forwarded-For`, no del primero, e ignora la cabecera estándar `Forwarded`. Ver "La IP del visitante" más abajo — sin esto el cupo por IP del chat no valía nada.
- **`CsrfCookieFilter`** (sin `@Order`, el último): el `Mono<CsrfToken>` que Spring Security deja en el exchange es **perezoso** — la cookie `XSRF-TOKEN` no se escribe hasta que alguien se suscribe, y aquí no hay plantilla de servidor que lo haga. Este filtro se suscribe. Solo en `/api/**`: son las únicas respuestas `no-store`, y emitir la cookie junto a un asset `immutable` dejaría que una caché compartida sirviera **el mismo token a todos los visitantes**. Va sin `@Order` (el último) porque tiene que correr por detrás del `WebFilterChainProxy` de Spring Security (orden `-100`), que es quien pone el atributo.
- **Cookie de sesión** (`SecurityConfig.webSessionIdResolver`): `HttpOnly` + `SameSite=Lax` + `Secure` condicional (`session.cookie.secure`, o `SESSION_COOKIE_SECURE=true` en Railway). Secure no puede ir fijo porque en local se sirve por HTTP y el navegador descartaría la cookie. **Quien crea sesiones ahora es solo `ChatRateLimitFilter`**, y únicamente después de pasar los cupos diario y de IP — así el número de sesiones al día queda acotado por el propio cupo. Ver "El almacén de sesiones" abajo.
- **`SecurityConfig`**: CSRF de Spring **activo** para el POST del chat, con `CookieServerCsrfTokenRepository.withHttpOnlyFalse()` (el token lo tiene que leer el JS del navegador) y `ServerCsrfTokenRequestAttributeHandler` **plano**: el handler por defecto en Spring Security 7 es el XOR (protección BREACH), que enmascara el token por petición y lo espera enmascarado de vuelta — incompatible con un cliente que devuelve el valor tal cual lo lee de la cookie. BREACH no aplica aquí: el token no se incrusta en el HTML comprimido, viaja en un `Set-Cookie`. La cookie `XSRF-TOKEN` sale con `SameSite=Lax` y `Secure` condicional, igual que la de sesión. CSP propia (`script-src 'self'`, `font-src 'self'`, `object-src 'none'`, `frame-ancestors 'none'`, etc.), todo `permitAll`. También desactiva el `cache()` por defecto de Spring Security, que ponía `no-store` en **toda** respuesta e impedía cachear los estáticos.
- **`CacheControlFilter`** (`@Order(-90)`): fija `Cache-Control` por ruta en `beforeCommit` (para ganar al manejador de estáticos):
  - `/assets/**` y `/fonts/**` → `public, max-age=31536000, immutable` (Vite pone hash de contenido en el nombre; **sustituir una fuente obliga a renombrarla**).
  - `/img/**`, `/docs/**`, `/favicon.svg` → `public, max-age=86400` (nombres estables que sí cambian: avatar, CV).
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
- `CustomCursor` — cursor custom con lag (solo mouse fino).
- `Navbar` — se vuelve sólido tras 40px de scroll; hamburguesa togglea el drawer; incluye `LanguageToggle`.
- `LanguageToggle` — botones ES/EN (banderas SVG inline, no emoji) que llaman a `setLang` del `LanguageContext`.
- `MobileDrawer` — drawer mobile (estado en `App`, cierra con Escape / al navegar). Incluye una entrada al chat de IA: en móvil el botón flotante es un icono fácil de pasar por alto, así que el menú es la segunda vía de entrada. Mientras el drawer está abierto, `App` esconde el botón flotante (`hidden`) para no duplicar la misma acción.
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
- `GitHubServiceTest` — unitario, sin red: construye el `WebClient` con `exchangeFunction(...)` fake para simular respuestas de GitHub. Cubre filtrado (forks / repo homónimo / sin descripción), mapeo a `RepoDTO`, fallback ante error, caché (no repite la llamada HTTP), el header `Authorization: Bearer` condicionado a que haya token, y la curación de topics (genéricos descartados, prioridad conceptual, hueco reservado al stack, sinónimos deduplicados, repo sin topics → lista vacía). También `getAllRepos()`: devuelve todos los públicos sin recortar a los destacados, aplica los mismos filtros, cachea aparte y cae al respaldo si GitHub falla.
- `CsrfValidationFilterTest` — unitario sobre el `WebFilter` con `MockServerWebExchange`: cookie y cabecera coincidentes dejan pasar; sin cabecera, sin cookie o sin coincidir → 404; rutas distintas de `/api/projects` no se validan, y `/api/chat` **no pasa por aquí** (lo cubre el CSRF nativo).
- `TrustedClientIpTransformerTest` — el arreglo de la suplantación de IP: con la IP falsa delante gana la que añade el proxy (una o varias), sin cabecera del cliente se usa la del proxy, la cabecera estándar `Forwarded` se ignora, se toleran espacios y valores vacíos, y **`X-Forwarded-Proto` sigue aplicándose** (si no, se perdería el HSTS en producción). Y el detector de "más de un proxy delante": qué direcciones cuentan como visitante y cuáles no (privadas, CGNAT, link-local, `fc00::/7`, o texto que no es una IP), incluido que comprobarlo **no resuelve por DNS** un valor que llega en una cabecera.
- `ChatServiceTest` — el prompt de sistema lleva reglas + perfil + los repos de GitHub (con lenguaje y etiquetas), las descripciones largas se recortan a 220 caracteres para no inflar el prompt, y los errores **se propagan** (traducirlos es cosa del advice). Sobre el historial: viaja como transcripción dentro del turno del visitante (con las etiquetas `Visitante:`/`Asistente:` y el aviso de que puede estar alterada), sin historial el turno es solo la pregunta, y —lo que cierra el agujero— **ningún mensaje de la conversación acaba con rol `assistant`** aunque el cliente lo pida.
- `ChatExceptionHandlerTest` — cada `statusCode` produce su mensaje (429 límite, 402 sin crédito, config/red genérico) y siempre con 200 + `text/event-stream`.
- `ChatControllerTest` — pregunta vacía no llega al modelo, recorte a 500 caracteres, historial recortado a 6 turnos, **cada turno recortado por separado** (500 el del visitante, 2.000 el del asistente, con 100 KB de entrada) sin tocar un historial de tamaño normal, y un turno con rol `system` degradado a `user`.
- `ChatRateLimitFilterTest` — cupo por sesión: dentro pasa, al superarlo 429 sin llamar al modelo, sesiones distintas no comparten cupo, otras rutas no consumen. Y los cupos por red: en IPv6 van por **/64** (rotar la dirección dentro del prefijo no estrena cupo) sin que dos prefijos distintos se pisen, una IPv4 mapeada (`::ffff:…`) sigue contando como IPv4, y un valor que no es una IP literal se agrupa tal cual en vez de romper el cupo. Del cupo diario por red: una sola no se lleva el presupuesto del día, otras redes no se ven afectadas, y **lo que frena el cupo de la hora no gasta el del día**.
- `CacheControlFilterTest` — la política de caché por ruta: assets con hash y fuentes inmutables, imágenes/CV a un día, y `index.html` + `/api/**` sin cachear nunca (esto último es el que protege los despliegues).
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
- `components/MobileDrawer.test.jsx` — el menú móvil ofrece el chat (traducido) y avisa a `App` al pulsarlo.
- `components/Chat.test.jsx` — el panel lo controla `App`: cerrado solo se ve el lanzador, `open` lo abre desde fuera y `hidden` esconde el lanzador con el menú abierto.
- `components/RichText.test.jsx`, `LanguageToggle.test.jsx`, `Contact.test.jsx`, `Footer.test.jsx` — comportamiento observable: negritas → `<strong>`, botón de idioma activo/click, CV descargable por idioma, año dinámico.
- `api/streamChat.test.js` — el parser SSE: reconstruye el texto, aguanta que un evento llegue troceado entre lecturas, une varias líneas `data:` de un mismo evento, manda el token en `X-XSRF-TOKEN`, y distingue 429 del resto.
- `hooks/useChat.test.js` — turno de usuario + turno del asistente rellenándose con el stream, preguntas vacías ignoradas, token CSRF reutilizado, y mensajes de límite/error sin romper la UI.
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
- **`ChatExceptionHandler`** (`@RestControllerAdvice`): traduce `AiClientException` a texto útil con **200**, no a un error. Distingue 429 (límite), 402 (sin crédito) y el resto. **Limitación real**: solo captura fallos *previos al primer token* (sin API key, 401, 429, red), que son los habituales porque `stream(...)` falla en la petición inicial. Un fallo a mitad de stream (`STREAM_ERROR`) llega con la respuesta ya comprometida y el visitante vería la respuesta truncada.
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
- `ai.model=${AI_MODEL:google/gemma-4-31b-it:free}` — el tier gratuito de OpenRouter son **20 req/min y 50 req/día** (1.000/día si alguna vez se compran $10 en créditos). Al agotarse entra el mensaje de respaldo.
- `ai.read-timeout=25s` — seguro **porque hay streaming**: el primer token llega rápido. En una llamada no-streaming OpenRouter no envía nada hasta terminar de generar, y 25s mataría respuestas largas.

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
- **Comentarios solo si son necesarios**: se comenta el *porqué* de una decisión no evidente (un workaround, una restricción externa, una alternativa descartada), nunca el *qué* hace el código. Si el comentario se limita a repetir lo que ya dice el nombre de la función o la línea siguiente, sobra.

## Notas / deuda técnica conocida
- **`spring.codec.max-in-memory-size` sigue en su valor por defecto (256 KB)**, que es el techo del cuerpo de `/api/chat`. No se baja a propósito: esa propiedad la aplica Spring Boot también al `WebClient`, y la lista completa de repos de GitHub (`per_page=100`) puede acercarse a ese tamaño. Lo que acota el coste del prompt son los topes por turno del controller, no esta propiedad.
- **Java 25 requerido para buildear** (`java.version=25` en el `pom.xml`, igual que el Dockerfile). Antes las propiedades decían 21 y el plugin forzaba 25, lo que hacía creer que bastaba un JDK 21.
- Links del drawer y del `#contact` ya apuntan a los perfiles reales (`github.com/adrian0511`, `linkedin.com/in/adrdev`).
- Estilos inline ya migrados a CSS en `ProjectCard` (`.pc-link`), `GithubCard` (`.gh-*`) y `Hero` (`.photo-stack`). Quedan algunos sueltos en `Projects` (mensaje de error).
- CSS global pendiente de pasar a CSS Modules de forma incremental.
- **CI en GitHub Actions** (`.github/workflows/ci.yml`): en cada push/PR a `main` o `dev` corre tests de backend, tests + build de frontend, y por último el `mvnw package` completo (el mismo que ejecuta Railway al desplegar). Declara `permissions: contents: read`: los jobs solo leen el repo, y sin ese bloque el `GITHUB_TOKEN` hereda el permiso por defecto del repositorio, que puede ser de escritura.
- **Dependabot** (`.github/dependabot.yml`): maven y npm semanales, actions mensual. Existe porque los avisos de dependencias solo aparecían si alguien lanzaba `npm audit` a mano. Con una excepción anotada: **no se aceptan subidas de línea de Spring Boot** (mayor/menor), solo parches de la 4.0.x — ver la nota de Spring Cloud más abajo.
- **`commons-fileupload` excluido del `pom.xml`**: llegaba por `prompt-link` → Spring Cloud OpenFeign → `feign-form-spring`, y solo sirve para subir ficheros multipart por Feign (aquí las llamadas a OpenRouter son JSON). La rama 1.x está sin mantenimiento y arrastra CVE-2025-48976, así que viajaba dentro del jar únicamente para que cualquier escáner de la imagen la marcara. Comprobado tras excluirla: el chat responde entero, sin `NoClassDefFoundError`.
- **Arranque limpio en producción**: se excluye `ReactiveUserDetailsServiceAutoConfiguration` (la app no autentica a nadie, así que el usuario en memoria de Spring Security y su password aleatoria solo ensuciaban el log) y el `ENTRYPOINT` del Dockerfile pasa `--enable-native-access=ALL-UNNAMED`, que desde Java 24 hace falta para que Netty no avise en cada arranque.
- **Mockito se pasa como `-javaagent`** (surefire + `dependency:properties` en el `pom.xml`): auto-adjuntarse dejará de funcionar en JDKs futuros y avisaba en cada build.
- **`mvnw` tiene que estar en el índice de git como `100755`**: creado desde Windows entró como `100644` y el runner de Linux respondía `Permission denied` (el paso fallaba en 0 s, sin llegar a compilar). Si se vuelve a añadir el wrapper: `git update-index --chmod=+x mvnw`.
- **No se puede subir a Spring Boot 4.1.x**: `prompt-link` arrastra Spring Cloud OpenFeign, y el verificador de compatibilidad de Spring Cloud 2025.1.x aborta el arranque con *"Spring Boot [4.1.1] is not compatible with this Spring Cloud release train — change to [4.0.x]"*. Comprobado: con 4.1.1 fallan los 4 tests que levantan contexto (los unitarios pasan, porque no arrancan Spring). Para subir habría que actualizar antes `prompt-link` a un release train de Spring Cloud compatible con Boot 4.1. **4.0.8 es la última versión usable.**
- **`vite` (devDependency) con vulnerabilidades conocidas** (moderate/high, vía `npm audit`): afectan solo al servidor de desarrollo (`vite dev`), no al build de producción que sirve Spring Boot. Actualizar a Vite 8 es un cambio mayor (breaking) pendiente de evaluar aparte.
- **ESLint configurado** (`frontend/eslint.config.js`, flat config con react-hooks): `npm run lint`.
- **`pushed_at` no es la fecha del último commit**: GitHub lo actualiza con cualquier push a *cualquier* rama (ramas de Dependabot, borrado de ramas…), así que el "Actualizado hace X" de las tarjetas puede indicar actividad que no es del autor. La fecha real sería `GET /repos/{owner}/{repo}/commits?per_page=1` (una llamada extra por repo). Se optó por mantener `pushed_at` por simplicidad.
- `hreflang` (es / en / x-default) declarado en `index.html`. Las tres alternativas apuntan a la misma URL porque el idioma se cambia en cliente.
