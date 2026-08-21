// Diccionarios de traducción (es / en).
// Convención: el texto puede llevar **negritas**, que <RichText> convierte en
// <strong> construyendo nodos React (no usa innerHTML, así que es seguro).
// Los nombres de tecnologías del stack no se traducen.

export const translations = {
  es: {
    nav: {
      about: 'Sobre mí',
      projects: 'Proyectos',
      contact: 'Contacto',
      menu: 'Menú',
    },
    hero: {
      badge: 'Disponible para trabajar',
      greeting: 'Hola, soy',
      role: 'Backend Developer.',
      desc:
        'Desarrollador backend enfocado en **APIs y arquitecturas distribuidas** escalables y resilientes. ' +
        'Núcleo en **Java & Spring** —microservicios, Kafka, seguridad— y expandiendo hacia Python y Node.js.',
      ctaProjects: 'Ver proyectos',
      ctaContact: 'Contáctame',
      chips: ['Open to work', 'Backend', 'Microservicios'],
      noPhoto: 'Sin foto de perfil',
    },
    about: {
      label: '01 — Sobre mí',
      heading: ['Backend,', 'arquitectura', 'y buenas prácticas.'],
      p1:
        'Desarrollador backend enfocado en construir **APIs robustas** y sistemas bien diseñados con ' +
        '**Java y Spring**. Me importa que el software sea **escalable, resiliente** y fácil de mantener.',
      p2:
        'Trabajo arquitecturas de **microservicios y sistemas distribuidos** —Spring Cloud, API Gateway, ' +
        'Eureka, Kafka, Resilience4j— con foco en **seguridad** (Spring Security, OAuth2). En paralelo, ' +
        'expando hacia **Python (FastAPI)** y **Node.js (NestJS)** para no atarme a un solo ecosistema.',
      p3: 'Busco un **equipo** donde seguir creciendo y aportar valor real desde el primer día.',
      stackLabel: 'Stack',
      legend: {
        core: 'Dominado',
        distributed: 'Microservicios / distribuido',
        learning: 'Python / Node.js (aprendiendo)',
      },
    },
    projects: {
      label: '02 — Proyectos',
      heading: ['Cosas que', 'he construido.'],
      errorTitle: 'No se pudieron cargar los proyectos',
      errorSub: 'Puedes verlos directamente en GitHub',
      fallbackDesc: 'Proyecto backend con Spring Boot',
      more: 'Más proyectos en GitHub',
      updated: 'Actualizado',
    },
    contact: {
      label: '03 — Contacto',
      heading: ['Trabajemos', 'juntos.'],
      sub:
        '¿Tienes un proyecto backend interesante o quieres hablar de arquitectura de software? ' +
        'Escríbeme, estaré encantado de hablar contigo.',
      cv: 'Descargar CV',
    },
    footer: {
      tagline: 'Backend Developer, Java & Spring',
    },
  },

  en: {
    nav: {
      about: 'About',
      projects: 'Projects',
      contact: 'Contact',
      menu: 'Menu',
    },
    hero: {
      badge: 'Open to work',
      greeting: "Hi, I'm",
      role: 'Backend Developer.',
      desc:
        'Backend developer focused on scalable, resilient **APIs and distributed architectures**. ' +
        'Core in **Java & Spring** —microservices, Kafka, security— and expanding into Python and Node.js.',
      ctaProjects: 'View projects',
      ctaContact: 'Get in touch',
      chips: ['Open to work', 'Backend', 'Microservices'],
      noPhoto: 'No profile photo',
    },
    about: {
      label: '01 — About',
      heading: ['Backend,', 'architecture', 'and best practices.'],
      p1:
        'Backend developer focused on building **robust APIs** and well-designed systems with ' +
        '**Java and Spring**. I care about software being **scalable, resilient** and easy to maintain.',
      p2:
        'I work with **microservices and distributed systems** architectures —Spring Cloud, API Gateway, ' +
        'Eureka, Kafka, Resilience4j— with a focus on **security** (Spring Security, OAuth2). In parallel, ' +
        "I'm expanding into **Python (FastAPI)** and **Node.js (NestJS)** so I'm not tied to a single ecosystem.",
      p3: "I'm looking for a **team** where I can keep growing and deliver real value from day one.",
      stackLabel: 'Stack',
      legend: {
        core: 'Proficient',
        distributed: 'Microservices / distributed',
        learning: 'Python / Node.js (learning)',
      },
    },
    projects: {
      label: '02 — Projects',
      heading: ['Things', "I've built."],
      errorTitle: "Couldn't load the projects",
      errorSub: 'You can see them directly on GitHub',
      fallbackDesc: 'Backend project with Spring Boot',
      more: 'More projects on GitHub',
      updated: 'Updated',
    },
    contact: {
      label: '03 — Contact',
      heading: ["Let's work", 'together.'],
      sub:
        'Got an interesting backend project, or want to talk software architecture? ' +
        "Feel free to reach out — I'd be glad to talk.",
      cv: 'Download CV',
    },
    footer: {
      tagline: 'Backend Developer, Java & Spring',
    },
  },
}
