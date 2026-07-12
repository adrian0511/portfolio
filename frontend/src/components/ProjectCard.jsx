import { useLanguage } from '../i18n/LanguageContext.jsx'

// Tarjeta de proyecto individual. Los datos (name/description) vienen de la API
// de GitHub, así que no se traducen; sí el texto de respaldo si falta descripción.
export default function ProjectCard({ repo, index }) {
  const { t } = useLanguage()
  const num = String(index + 1).padStart(2, '0')

  return (
    <div className="pc rv">
      <a
        href={repo.html_url}
        target="_blank"
        rel="noreferrer"
        style={{
          textDecoration: 'none',
          color: 'inherit',
          display: 'flex',
          flexDirection: 'column',
          gap: '.85rem',
          height: '100%',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="pnum">{num}</span>
          <span className="ptag">{repo.topic || 'Backend'}</span>
        </div>

        <h3>{repo.name}</h3>
        <p className="pdesc">{repo.description || t.projects.fallbackDesc}</p>

        <div className="plinks">
          <span className="pl">GitHub →</span>
        </div>
      </a>
    </div>
  )
}
