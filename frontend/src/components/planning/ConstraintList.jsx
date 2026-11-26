const STATUS = {
  todo: { label: 'À compléter', icon: <><circle cx="8" cy="8" r="6" /><path d="M8 4.8v3.8M8 11v.1" /></> },
  warn: { label: 'À vérifier', icon: <><path d="M8 2.2 14.2 13H1.8Z" /><path d="M8 6.6v2.8M8 11.3v.1" /></> },
  server: { label: 'Vérifié à l’enregistrement', icon: <><circle cx="8" cy="8" r="6" strokeDasharray="2.4 2.4" /><path d="M8 5v3.2l2 1.2" /></> },
  ok: { label: 'En ordre', icon: <><circle cx="8" cy="8" r="6" /><path d="m5.4 8.2 1.8 1.8 3.4-3.8" /></> },
}
const ORDER = Object.keys(STATUS)

// open: controlled by the builder, which opens it when a save finds something missing
export default function ConstraintList({ rules, open, onToggle, onOpen }) {
  const left = rules.filter(({ status }) => status === 'todo').length
  const sorted = [...rules].sort((a, b) => ORDER.indexOf(a.status) - ORDER.indexOf(b.status))
  return (
    <section className="constraints" aria-label="Contrôles de la tournée">
      <button className="constraints-toggle" type="button" aria-expanded={open} onClick={onToggle}>
        <span>Contrôles</span>
        <span role="status">{left === 0 ? 'Rien à compléter' : `${left} à compléter`}</span>
        <svg className="constraints-chevron" viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m4 10 4-4 4 4" /></svg>
      </button>
      {open && (
        <ul>
          {sorted.map(({ section, status, label, detail }) => (
            <li key={label}>
              <button type="button" className={`constraint ${status}`} onClick={() => onOpen(section)}>
                <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{STATUS[status].icon}</svg>
                <span><span className="constraint-label">{label}<span className="visually-hidden"> : {STATUS[status].label}</span></span>{detail && <span className="muted">{detail}</span>}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
