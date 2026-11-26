// Numbers that are also filters. labels: { STATUS: text }; counts: { STATUS: n } or null while loading
export default function FilterCounts({ allLabel, labels, total, counts, value, onChange }) {
  return (
    <div className="counts" role="group" aria-label="Filtrer par statut" style={{ '--columns': Object.keys(labels).length + 1 }}>
      {[['', allLabel], ...Object.entries(labels)].map(([status, label]) => (
        <button key={status} type="button" className={`count-card ${status}`} aria-pressed={value === status} onClick={() => onChange(status)}>
          <span className="count-number">{counts ? (status ? counts[status] ?? 0 : total) : '–'}</span>
          {label}
        </button>
      ))}
    </div>
  )
}
