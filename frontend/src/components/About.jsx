import { useLanguage } from '../i18n/LanguageContext.jsx'
import RichText from './RichText.jsx'
import {
  SiOpenjdk,
  SiSpringboot,
  SiSpringsecurity,
  SiPostgresql,
  SiMysql,
  SiApachekafka,
  SiDocker,
  SiLinux,
  SiGithub,
  SiSpring,
  SiPython,
  SiFastapi,
  SiSqlalchemy,
  SiPydantic,
  SiNestjs,
  SiTypescript,
  SiReact,
  SiSequelize,
} from 'react-icons/si'

// tier: '' = dominado, 'l' = microservicios/distribuido, 's' = aprendiendo
// Icon solo cuando la tecnología tiene un logo de marca real reconocible;
// el resto se queda con el punto de color (ver CSS .sd).
const STACK = [
  { name: 'Java', tier: '', Icon: SiOpenjdk },
  { name: 'Spring Boot', tier: '', Icon: SiSpringboot },
  { name: 'Spring Security', tier: '', Icon: SiSpringsecurity },
  { name: 'REST APIs', tier: '' },
  { name: 'PostgreSQL', tier: '', Icon: SiPostgresql },
  { name: 'MySQL', tier: '', Icon: SiMysql },
  { name: 'Kafka', tier: '', Icon: SiApachekafka },
  { name: 'Docker', tier: '', Icon: SiDocker },
  { name: 'Linux', tier: '', Icon: SiLinux },
  { name: 'Git / GitHub', tier: '', Icon: SiGithub },

  { name: 'Spring Cloud', tier: 'l', Icon: SiSpring },
  { name: 'API Gateway', tier: 'l' },
  { name: 'Eureka', tier: 'l' },
  { name: 'Resilience4j', tier: 'l' },
  { name: 'OAuth2', tier: 'l' },

  { name: 'Python', tier: 's', Icon: SiPython },
  { name: 'FastAPI', tier: 's', Icon: SiFastapi },
  { name: 'SQLAlchemy', tier: 's', Icon: SiSqlalchemy },
  { name: 'Pydantic', tier: 's', Icon: SiPydantic },
  { name: 'NestJS', tier: 's', Icon: SiNestjs },
  { name: 'TypeScript', tier: 's', Icon: SiTypescript },
  { name: 'React', tier: 's', Icon: SiReact },
  { name: 'Sequelize', tier: 's', Icon: SiSequelize },
]

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
          {STACK.map(({ name, tier, Icon }) => (
            <div key={name} className={tier ? `si ${tier}` : 'si'}>
              {Icon ? <Icon className="si-icon" aria-hidden="true" /> : <span className="sd"></span>}
              {name}
            </div>
          ))}
        </div>
        <div className="sleg">
          <span><span className="sld" style={{ background: 'var(--accent)' }}></span>{t.about.legend.core}</span>
          <span><span className="sld" style={{ background: 'var(--green)' }}></span>{t.about.legend.distributed}</span>
          <span><span className="sld" style={{ background: '#fbbf24' }}></span>{t.about.legend.learning}</span>
        </div>
      </div>
    </section>
  )
}
