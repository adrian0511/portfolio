import { useLanguage } from '../i18n/LanguageContext.jsx'

export default function GithubCard() {
  const { t } = useLanguage()

  return (
    <div className="pc rv gh-card">
      <div className="gh-plus">+</div>
      <div className="gh-more">{t.projects.more}</div>
      <a href="https://github.com/adrian0511" target="_blank" rel="noreferrer" className="gh-link">
        @adrian0511 →
      </a>
    </div>
  )
}
