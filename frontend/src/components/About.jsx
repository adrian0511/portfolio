// Seccion Sobre mí + grid de stack. Sin .rv (reveal) todavia: se agrega en el paso 4.
export default function About() {
  return (
    <section id="about">
      <div className="rv">
        <div className="sec-lbl">01 — Sobre mí</div>
        <h2>Backend,<br />arquitectura<br />y café. ☕</h2>
        <div className="about-txt">
          <p>Soy estudiante de <strong>Ingeniería de Software / Sistemas</strong> apasionado por el desarrollo backend. Me
            especializo en construir APIs robustas y sistemas bien diseñados con <strong>Java y Spring Framework</strong>.
          </p>
          <p>Manejo arquitecturas <strong>monolíticas con Spring Boot</strong>, y estoy aprendiendo a diseñar
            <strong> microservicios</strong> con Feign, Eureka y Spring Cloud Gateway. Actualmente profundizando en
            <strong> OAuth2 y Kafka</strong>.
          </p>
          <p>Busco <strong>trabajo</strong> donde pueda seguir creciendo junto a un equipo y aportar valor real desde el
            primer día.</p>
        </div>
      </div>

      <div className="rv">
        <div className="sec-lbl">Stack</div>
        <div className="sg">
          {/* Dominado */}
          <div className="si"><span className="sd"></span>Java 21 / 25</div>
          <div className="si"><span className="sd"></span>Spring Boot</div>
          <div className="si"><span className="sd"></span>Spring MVC</div>
          <div className="si"><span className="sd"></span>Spring Data JPA</div>
          <div className="si"><span className="sd"></span>Hibernate</div>
          <div className="si"><span className="sd"></span>PostgreSQL</div>
          <div className="si"><span className="sd"></span>MySQL</div>
          <div className="si"><span className="sd"></span>REST APIs</div>
          {/* Aprendiendo microservicios */}
          <div className="si l"><span className="sd"></span>Feign Client</div>
          <div className="si l"><span className="sd"></span>Eureka</div>
          <div className="si l"><span className="sd"></span>API Gateway</div>
          <div className="si l"><span className="sd"></span>Spring Cloud</div>
          {/* Estudiando */}
          <div className="si s"><span className="sd"></span>OAuth2</div>
          <div className="si s"><span className="sd"></span>Kafka</div>
          <div className="si s"><span className="sd"></span>Docker</div>
          <div className="si s"><span className="sd"></span>Git / GitHub</div>
        </div>
        <div className="sleg">
          <span><span className="sld" style={{ background: 'var(--accent)' }}></span>Dominado</span>
          <span><span className="sld" style={{ background: 'var(--blue)' }}></span>Microservicios (aprendiendo)</span>
          <span><span className="sld" style={{ background: '#a78bfa' }}></span>Estudiando</span>
        </div>
      </div>
    </section>
  )
}
