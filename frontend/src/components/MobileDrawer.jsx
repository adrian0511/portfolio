// Drawer mobile. La apertura/cierre la controla App (estado drawerOpen).
// Cada link cierra el drawer al navegar (equivalente a closeDrawer()).
export default function MobileDrawer({ open, onClose }) {
  return (
    <div className={`drawer${open ? ' open' : ''}`} id="drawer">
      <a href="#about" onClick={onClose}>Sobre mí</a>
      <a href="#projects" onClick={onClose}>Proyectos</a>
      <a href="#contact" onClick={onClose}>Contacto</a>
      <div className="drawer-sub">
        <a href="https://github.com/adrian0511" target="_blank" rel="noreferrer">GitHub</a>
        <a href="https://linkedin.com/in/adrdev" target="_blank" rel="noreferrer">LinkedIn</a>
      </div>
    </div>
  )
}
