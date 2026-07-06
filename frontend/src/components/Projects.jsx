import ProjectCard from './ProjectCard.jsx'
import GithubCard from './GithubCard.jsx'
import useProjects from '../hooks/useProjects.js'
import useRevealOnScroll from '../hooks/useRevealOnScroll.js'

// Seccion Proyectos: consume /api/csrf-token -> /api/projects (ver useProjects).
// - loading: solo la tarjeta de GitHub (como el estado inicial del HTML original)
// - success: una ProjectCard por repo + tarjeta de GitHub
// - error:   mensaje de fallback + tarjeta de GitHub
export default function Projects() {
  const { status, repos } = useProjects()

  // Re-escanea el reveal-on-scroll cuando aparecen las tarjetas dinamicas.
  useRevealOnScroll([status, repos.length])

  return (
    <section id="projects">
      <div className="sec-hdr rv">
        <div className="sec-lbl">02 — Proyectos</div>
        <h2>Cosas que<br />he construido.</h2>
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
            <h3 style={{ color: 'var(--muted)' }}>No se pudieron cargar los proyectos</h3>
            <p className="pdesc">Puedes verlos directamente en GitHub</p>
          </div>
        )}

        <GithubCard />
      </div>
    </section>
  )
}
