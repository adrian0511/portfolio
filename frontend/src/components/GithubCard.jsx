import { useLanguage } from '../i18n/LanguageContext.jsx'

export default function GithubCard() {
  const { t } = useLanguage()

  return (
    <div
      className="pc rv"
      style={{
        border: '1px dashed var(--border2)',
        background: 'transparent',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        gap: '.5rem',
        minHeight: '180px',
      }}
    >
      <div style={{ fontSize: '1.3rem', color: 'var(--muted)' }}>+</div>
      <div style={{ fontSize: '.72rem', color: 'var(--muted2)' }}>{t.projects.more}</div>
      <a
        href="https://github.com/adrian0511"
        target="_blank"
        rel="noreferrer"
        style={{
          fontSize: '.63rem',
          color: 'var(--accent)',
          textDecoration: 'none',
          letterSpacing: '.1em',
          textTransform: 'uppercase',
          marginTop: '.35rem',
        }}
      >
        @adrian0511 →
      </a>
    </div>
  )
}
