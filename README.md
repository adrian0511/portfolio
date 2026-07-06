# Adrián Garcés — Portfolio Backend Dev

Portfolio personal desarrollado con **Spring Boot** y **WebFlux**, que consume la API de [GitHub](https://github.com/) para mostrar tus proyectos dinámicamente. Incluye diseño responsivo, manejo de errores, timeouts y fallback para conexiones lentas.

---

## 🌟 Descripción

Este portfolio muestra mis proyectos, skills y experiencia como desarrollador backend.  
Construido con buenas prácticas de **Java 25**, **Spring Boot**, **WebFlux** y arquitecturas modernas. Perfecto para mostrar mi trabajo a reclutadores o clientes.

---

## 💻 Stack Tecnológico

**Backend:**  
- Java 25  
- Spring Boot  
- Spring WebFlux  
- REST APIs  

**Frontend:**  
- React 18 + Vite (JavaScript)  
- CSS3 (global, con variables), diseño responsivo y animaciones  
- Consumo dinámico de la API de GitHub (flujo CSRF por sesión)  

El código del frontend vive en [`frontend/`](frontend/). Durante `./mvnw package` se
compila con Vite y se empaqueta dentro del jar (se sirve como estático en `/`).

---

## 🚀 Funcionalidades

- Muestra proyectos dinámicos desde tu GitHub  
- Fallback y mensajes si la API falla  
- Responsive para escritorio, tablet y móvil  
- Animaciones y cursor personalizado  
- Drawer mobile para navegación  
- Sección de contacto con enlaces a GitHub, LinkedIn y correo

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

---
