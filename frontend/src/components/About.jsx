import { useLanguage } from '../i18n/LanguageContext.jsx'
import RichText from './RichText.jsx'

export default function About() {
  const { t } = useLanguage()

  return (
    <section id="about">
      <div className="rv">
        <div className="sec-lbl">{t.about.label}</div>
        <h2>
          {t.about.heading.map((line, i) => (
            <span key={i}>{line}{i < t.about.heading.length - 1 && <br />}</span>
          ))}
        </h2>
        <div className="about-txt">
          <p><RichText>{t.about.p1}</RichText></p>
          <p><RichText>{t.about.p2}</RichText></p>
          <p><RichText>{t.about.p3}</RichText></p>
        </div>
      </div>

      <div className="rv">
        <div className="sec-lbl">{t.about.stackLabel}</div>
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
          <span><span className="sld" style={{ background: 'var(--accent)' }}></span>{t.about.legend.core}</span>
          <span><span className="sld" style={{ background: 'var(--blue)' }}></span>{t.about.legend.distributed}</span>
          <span><span className="sld" style={{ background: '#a78bfa' }}></span>{t.about.legend.learning}</span>
        </div>
      </div>
    </section>
  )
}
