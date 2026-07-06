// Seccion Sobre mí + grid de stack. Sin .rv (reveal) todavia: se agrega en el paso 4.
export default function About() {
  return (
    <section id="about">
      <div className="rv">
        <div className="sec-lbl">01 — Sobre mí</div>
        <h2>Backend,<br />arquitectura<br />y café. ☕</h2>
        <div className="about-txt">
          <p>Desarrollador backend enfocado en construir <strong>APIs robustas</strong> y sistemas bien diseñados con
            <strong> Java y Spring</strong>. Me importa que el software sea <strong>escalable, resiliente</strong> y fácil
            de mantener.
          </p>
          <p>Trabajo arquitecturas de <strong>microservicios y sistemas distribuidos</strong> —Spring Cloud, API Gateway,
            Eureka, Kafka, Resilience4j— con foco en <strong>seguridad</strong> (Spring Security, OAuth2). En paralelo,
            expando hacia <strong>Python (FastAPI)</strong> y <strong>Node.js (NestJS)</strong> para no atarme a un solo
            ecosistema.
          </p>
          <p>Busco un <strong>equipo</strong> donde seguir creciendo y aportar valor real desde el primer día.</p>
        </div>
      </div>

      <div className="rv">
        <div className="sec-lbl">Stack</div>
        <div className="sg">
          {/* Dominado — core Java / Spring */}
          <div className="si"><span className="sd"></span>Java</div>
          <div className="si"><span className="sd"></span>Spring Boot</div>
          <div className="si"><span className="sd"></span>Spring Security</div>
          <div className="si"><span className="sd"></span>REST APIs</div>
          <div className="si"><span className="sd"></span>PostgreSQL</div>
          <div className="si"><span className="sd"></span>MySQL</div>
          <div className="si"><span className="sd"></span>Kafka</div>
          <div className="si"><span className="sd"></span>Docker</div>
          <div className="si"><span className="sd"></span>Linux</div>
          <div className="si"><span className="sd"></span>Git / GitHub</div>
          {/* Microservicios / distribuido */}
          <div className="si l"><span className="sd"></span>Spring Cloud</div>
          <div className="si l"><span className="sd"></span>API Gateway</div>
          <div className="si l"><span className="sd"></span>Eureka</div>
          <div className="si l"><span className="sd"></span>Resilience4j</div>
          <div className="si l"><span className="sd"></span>OAuth2</div>
          {/* Aprendiendo — Python / Node */}
          <div className="si s"><span className="sd"></span>Python</div>
          <div className="si s"><span className="sd"></span>FastAPI</div>
          <div className="si s"><span className="sd"></span>SQLAlchemy</div>
          <div className="si s"><span className="sd"></span>Pydantic</div>
          <div className="si s"><span className="sd"></span>NestJS</div>
          <div className="si s"><span className="sd"></span>TypeScript</div>
          <div className="si s"><span className="sd"></span>React</div>
          <div className="si s"><span className="sd"></span>Sequelize</div>
        </div>
        <div className="sleg">
          <span><span className="sld" style={{ background: 'var(--accent)' }}></span>Dominado</span>
          <span><span className="sld" style={{ background: 'var(--blue)' }}></span>Microservicios / distribuido</span>
          <span><span className="sld" style={{ background: '#a78bfa' }}></span>Python / Node.js (aprendiendo)</span>
        </div>
      </div>
    </section>
  )
}
