// Tarjeta de proyecto individual (reemplaza el innerHTML de renderProjects()).
export default function ProjectCard({ repo, index }) {
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
        <p className="pdesc">{repo.description || 'Proyecto backend con Spring Boot'}</p>

        <div className="plinks">
          <span className="pl">GitHub →</span>
        </div>
      </a>
    </div>
  )
}
