# Adrián Garcés — Portfolio

Portfolio personal: un backend **Spring Boot (WebFlux)** que sirve el frontend
**React** compilado, lee mis repositorios de la API de GitHub y lleva un
asistente de IA que responde preguntas sobre mi perfil.

---

## 🌟 Descripción

Soy **estudiante del ciclo de Desarrollo de Aplicaciones Web (DAW)** y construyo
backend con Java y Spring en proyectos propios. Esta web es uno de ellos: no es
una plantilla con mis datos encima, está escrita para resolver los problemas que
trae servir una página real —caché, límites de uso, streaming, fallbacks— y para
que se pueda leer el porqué de cada decisión.

Lo que la web enseña (microservicios, Kafka, seguridad) sale de esos proyectos,
no de un puesto de trabajo.

---

## 💻 Stack

**Backend**
- Java 25, Spring Boot 4.0.x
- Spring WebFlux (reactivo, `Mono`/`Flux`) y Spring Security
- [`prompt-link`](https://github.com/adrian0511/prompt-link) — mi propia librería
  publicada en Maven Central, que enruta a OpenRouter y aporta el streaming del chat
- Maven (wrapper incluido)

**Frontend** (en [`frontend/`](frontend/))
- React 18 + Vite (JavaScript)
- CSS global con variables, diseño responsivo y animaciones
- i18n propio español/inglés: detección por navegador y persistencia
- Fuentes autoalojadas (variables, servidas con caché `immutable`)

Durante `./mvnw package` el frontend se compila con Vite y se empaqueta dentro
del jar, que lo sirve como estático en `/`.

---

## 🚀 Funcionalidades

- **Proyectos desde la API de GitHub**, con curación de etiquetas, caché en
  memoria y una lista de respaldo: si GitHub falla, la portada no se rompe
- **Chat de IA** sobre mi perfil, con streaming token a token por SSE,
  guardarraíles en el prompt para que no invente experiencia, y cupos de uso por
  sesión, por red y por día. Sin API key responde derivando al email
- Selector de idioma ES/EN y CV descargable en el idioma activo
- Cursor personalizado, reveal on scroll, drawer móvil y nav sólido al hacer scroll
- Cabeceras de seguridad propias (CSP, Permissions-Policy) y caché por ruta

---

## 🔐 Comunicación frontend ↔ backend

| Método | Ruta | Requiere | Respuesta |
|--------|------|----------|-----------|
| GET | `/api/csrf-token` | nada | `204` + cookie `XSRF-TOKEN` |
| GET | `/api/projects` | cabecera `X-XSRF-TOKEN` | `RepoDTO[]` |
| POST | `/api/chat` | cabecera `X-XSRF-TOKEN` + cupo | `text/event-stream` |

Hay **un solo token** en toda la app, el del CSRF de Spring Security, y vive en
la cookie, no en la sesión: guardarlo en la `WebSession` convertía
`/api/csrf-token` en una fábrica de sesiones, y con el almacén lleno (10.000) la
web respondía 500 a todo el mundo. El porqué de los dos mecanismos CSRF que
conviven está en [`CLAUDE.md`](CLAUDE.md).

---

## ✅ Tests

**Backend** (JUnit 5, Mockito, WebTestClient — sin red, todo mockeado):
```bash
./mvnw test
./mvnw test -Dskip.frontend=true   # solo Java, sin recrear node_modules
```

**Frontend** (Vitest + Testing Library):
```bash
cd frontend
npm test
npm run lint
```

Cubren el flujo CSRF de punta a punta, los cupos del chat, la resolución de la
IP del visitante detrás del proxy, la política de caché por ruta, el fallback de
`GitHubService` y los hooks y componentes con lógica propia. Detalle completo en
[`CLAUDE.md`](CLAUDE.md#testing).

---

## 🛠️ Cómo levantarlo

**Producción / build único** (compila React y lo empaqueta en el jar):

```bash
./mvnw clean package
java -jar target/portfolio-0.0.1-SNAPSHOT.jar   # http://localhost:8080
```

**Desarrollo** (backend y frontend por separado, con hot-reload de React):

```bash
./mvnw spring-boot:run          # backend en :8080
cd frontend && npm run dev       # frontend en :5173, proxy /api -> :8080
```

> El backend requiere **JDK 25** para compilar (`./mvnw` usa el `JAVA_HOME` que apunte a un JDK 25).

**Para que el chat responda** hace falta una clave de [OpenRouter](https://openrouter.ai)
en `config/application.properties` (en la raíz, git-ignorado; Spring Boot lee
`./config/` de serie y sus valores ganan a los del classpath):

```properties
ai.api-key=sk-or-...
```

Sin ella el chat no rompe: responde con su mensaje de respaldo derivando al
email. En producción la clave es la variable de entorno `OPENROUTER_API_KEY`.
Nunca en `frontend/.env`: Vite inlinaría la clave en el bundle público.

---

## 📄 El CV

Los PDF de `frontend/public/docs/` **se generan**, no se editan a mano:

```bash
pwsh cv/render.ps1
```

El HTML y el CSS de los que salen están en [`cv/`](cv/), y su diseño está
condicionado por el ATS que lee el CV antes que ninguna persona. Los detalles,
en [`cv/README.md`](cv/README.md).
