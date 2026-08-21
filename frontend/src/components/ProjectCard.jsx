import { useLanguage } from '../i18n/LanguageContext.jsx'
import {
  SiOpenjdk,
  SiJavascript,
  SiTypescript,
  SiPython,
  SiGo,
  SiKotlin,
  SiPhp,
  SiRuby,
  SiRust,
  SiHtml5,
  SiGnubash,
  SiDocker,
} from 'react-icons/si'

// Colores oficiales de GitHub (linguist); los no listados caen a gris neutro.
const LANGUAGE_COLORS = {
  Java: '#b07219',
  JavaScript: '#f1e05a',
  TypeScript: '#3178c6',
  Python: '#3572A5',
  Kotlin: '#A97BFF',
  Go: '#00ADD8',
  Ruby: '#701516',
  PHP: '#4F5D95',
  'C#': '#178600',
  HTML: '#e34c26',
  CSS: '#663399',
  Shell: '#89e051',
  Dockerfile: '#384d54',
}

// Los lenguajes sin logo caen al punto de color, que es la convención de GitHub.
const LANGUAGE_ICONS = {
  Java: SiOpenjdk,
  JavaScript: SiJavascript,
  TypeScript: SiTypescript,
  Python: SiPython,
  Go: SiGo,
  Kotlin: SiKotlin,
  PHP: SiPhp,
  Ruby: SiRuby,
  Rust: SiRust,
  HTML: SiHtml5,
  Shell: SiGnubash,
  Dockerfile: SiDocker,
}

// Las palabras que ya traen mayúsculas se dejan intactas para no romper
// nombres como "RetosConIA".
function toTitle(name) {
  return name
    .replace(/[-_]/g, ' ')
    .split(' ')
    .map((word) => (word === word.toLowerCase() ? word.charAt(0).toUpperCase() + word.slice(1) : word))
    .join(' ')
}

const UNITS = [
  ['year', 365 * 24 * 60 * 60 * 1000],
  ['month', 30 * 24 * 60 * 60 * 1000],
  ['week', 7 * 24 * 60 * 60 * 1000],
  ['day', 24 * 60 * 60 * 1000],
  ['hour', 60 * 60 * 1000],
  ['minute', 60 * 1000],
]

// Intl.RelativeTimeFormat ya localiza y pluraliza, así que no hacen falta
// cadenas de traducción propias para las unidades de tiempo.
function relativeTime(isoDate, lang) {
  const elapsed = Date.now() - new Date(isoDate).getTime()
  if (Number.isNaN(elapsed)) return null

  const rtf = new Intl.RelativeTimeFormat(lang, { numeric: 'auto' })
  for (const [unit, ms] of UNITS) {
    // Redondeo, no truncado: 3 días y 17 horas son "hace 4 días", no "hace 3".
    if (elapsed >= ms) return rtf.format(-Math.round(elapsed / ms), unit)
  }
  return rtf.format(0, 'day')
}

export default function ProjectCard({ repo, index }) {
  const { lang, t } = useLanguage()
  const num = String(index + 1).padStart(2, '0')
  const updated = repo.pushed_at ? relativeTime(repo.pushed_at, lang) : null
  const LangIcon = LANGUAGE_ICONS[repo.language]
  const langColor = LANGUAGE_COLORS[repo.language] || 'var(--muted2)'

  return (
    <div className="pc rv">
      <a href={repo.html_url} target="_blank" rel="noreferrer" className="pc-link">
        <div className="pc-head">
          <span className="pnum">{num}</span>
          {repo.language && (
            <span className="plang">
              {LangIcon ? (
                <LangIcon className="plang-icon" style={{ color: langColor }} aria-hidden="true" />
              ) : (
                <span className="plang-dot" style={{ background: langColor }}></span>
              )}
              {repo.language}
            </span>
          )}
        </div>

        <h3>{toTitle(repo.name)}</h3>
        <p className="pdesc">{repo.description || t.projects.fallbackDesc}</p>

        {repo.topics?.length > 0 && (
          <div className="ptags">
            {repo.topics.map((topic) => (
              <span key={topic} className="ptag">{topic}</span>
            ))}
          </div>
        )}

        <div className="plinks">
          <span className="pl">GitHub →</span>
          {updated && <span className="pupdated">{t.projects.updated} {updated}</span>}
        </div>
      </a>
    </div>
  )
}
