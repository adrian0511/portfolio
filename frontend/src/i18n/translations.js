// El texto puede llevar **negritas**: las convierte <RichText>, no innerHTML.
// Los nombres de tecnologías del stack no se traducen.

export const translations = {
  es: {
    nav: {
      about: 'Sobre mí',
      projects: 'Proyectos',
      contact: 'Contacto',
      chat: 'Preguntar a la IA',
      menu: 'Menú',
    },
    hero: {
      badge: 'Disponible para trabajar',
      greeting: 'Hola, soy',
      role: 'Estudiante de DAW.',
      desc:
        'Estudio **Desarrollo de Aplicaciones Web** y construyo **backend con Java & Spring** en ' +
        'proyectos propios, públicos en GitHub. Aprendiendo también Python y Node.js.',
      ctaProjects: 'Ver proyectos',
      ctaContact: 'Contáctame',
      chips: ['Open to work', 'Backend', 'Java'],
      noPhoto: 'Sin foto de perfil',
    },
    about: {
      label: '01 — Sobre mí',
      heading: ['Estudiante de DAW', 'que construye', 'proyectos de verdad.'],
      p1:
        'Estudio el ciclo de **Desarrollo de Aplicaciones Web** y aprendo backend por mi cuenta con ' +
        '**Java y Spring**. Me importa que el código esté **probado** y sea **fácil de mantener**.',
      p2:
        'En mis proyectos he montado **microservicios y sistemas distribuidos** —Spring Cloud, API Gateway, ' +
        'Eureka, Kafka, Resilience4j— con foco en **seguridad** (Spring Security, OAuth2). En paralelo ' +
        'aprendo **Python (FastAPI)** y **Node.js (NestJS)** para no atarme a un solo ecosistema.',
      p3: 'Busco un **equipo** donde aprender de gente con más experiencia y aportar desde el primer día.',
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
        '¿Tienes un proyecto backend o quieres hablar de código? ' +
        'Escríbeme, estaré encantado de hablar contigo.',
      cv: 'Descargar CV',
    },
    footer: {
      tagline: 'Estudiante de DAW · Backend con Java & Spring',
    },
    chat: {
      launcher: 'Pregunta sobre mí',
      title: 'Asistente del portfolio',
      badge: 'IA',
      open: 'Abrir el chat',
      close: 'Cerrar el chat',
      send: 'Enviar',
      thinking: 'Escribiendo',
      placeholder: 'Escribe tu pregunta…',
      intro:
        'Respondo con IA a partir del perfil de Adrián. No soy él y puedo equivocarme: ' +
        'para algo importante, escríbele.',
      samples: ['¿Qué está estudiando?', '¿Qué proyectos ha construido?', '¿Está disponible para trabajar?'],
      error: 'Ahora mismo no puedo responder. Escribe a adriangarces0310@gmail.com.',
      limit: 'Has alcanzado el límite de preguntas de esta sesión. Escribe a adriangarces0310@gmail.com.',
    },
  },

  en: {
    nav: {
      about: 'About',
      projects: 'Projects',
      contact: 'Contact',
      chat: 'Ask the AI',
      menu: 'Menu',
    },
    hero: {
      badge: 'Open to work',
      greeting: "Hi, I'm",
      role: 'Web Development student.',
      desc:
        "I'm studying Web Application Development (DAW) and building **backend with Java & Spring** " +
        'in my own projects, public on GitHub. Also learning Python and Node.js.',
      ctaProjects: 'View projects',
      ctaContact: 'Get in touch',
      chips: ['Open to work', 'Backend', 'Java'],
      noPhoto: 'No profile photo',
    },
    about: {
      label: '01 — About',
      heading: ['A student', 'building real', 'projects.'],
      p1:
        "I'm studying **Web Application Development (DAW)** and teaching myself backend with " +
        '**Java and Spring**. I care about code being **tested** and **easy to maintain**.',
      p2:
        'In my own projects I have built **microservices and distributed systems** —Spring Cloud, API ' +
        'Gateway, Eureka, Kafka, Resilience4j— with a focus on **security** (Spring Security, OAuth2). ' +
        "Alongside that I'm learning **Python (FastAPI)** and **Node.js (NestJS)** so I'm not tied to a single ecosystem.",
      p3: "I'm looking for a **team** where I can learn from more experienced people and contribute from day one.",
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
        'Got a backend project, or just want to talk code? ' +
        "Feel free to reach out — I'd be glad to talk.",
      cv: 'Download CV',
    },
    footer: {
      tagline: 'Web Development student · Backend with Java & Spring',
    },
    chat: {
      launcher: 'Ask about me',
      title: 'Portfolio assistant',
      badge: 'AI',
      open: 'Open the chat',
      close: 'Close the chat',
      send: 'Send',
      thinking: 'Typing',
      placeholder: 'Type your question…',
      intro:
        "I answer with AI based on Adrián's profile. I'm not him and I can be wrong: " +
        'for anything important, drop him a line.',
      samples: ['What is he studying?', 'What has he built?', 'Is he open to work?'],
      error: "I can't answer right now. Write to adriangarces0310@gmail.com.",
      limit: "You've reached this session's question limit. Write to adriangarces0310@gmail.com.",
    },
  },
}
