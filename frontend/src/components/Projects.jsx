import ProjectCard from './ProjectCard.jsx'
import GithubCard from './GithubCard.jsx'
import useProjects from '../hooks/useProjects.js'
import useRevealOnScroll from '../hooks/useRevealOnScroll.js'
import { useLanguage } from '../i18n/LanguageContext.jsx'

// Seccion Proyectos: consume /api/csrf-token -> /api/projects (ver useProjects).
export default function Projects() {
  const { status, repos } = useProjects()
  const { t } = useLanguage()

  // Re-escanea el reveal-on-scroll cuando aparecen las tarjetas dinamicas.
  useRevealOnScroll([status, repos.length])

  return (
    <section id="projects">
      <div className="sec-hdr rv">
        <div className="sec-lbl">{t.projects.label}</div>
        <h2>
          {t.projects.heading.map((line, i) => (
            <span key={i}>{line}{i < t.projects.heading.length - 1 && <br />}</span>
          ))}
        </h2>
      </div>

      <div className="pg" id="projects-container">
        {status === 'success' &&
          repos.map((repo, i) => (
            <ProjectCard key={repo.html_url || repo.name} repo={repo} index={i} />
          ))}

        {status === 'error' && (
          <div
            className="pc rv"
            style={{
              gridColumn: 'span 3',
              textAlign: 'center',
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            <h3 style={{ color: 'var(--muted)' }}>{t.projects.errorTitle}</h3>
            <p className="pdesc">{t.projects.errorSub}</p>
          </div>
        )}

        <GithubCard />
      </div>
    </section>
  )
}
