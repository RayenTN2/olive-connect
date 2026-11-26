export default function TeamLeaderField({ leader, resources, onChange }) {
  const free = (resources.data ?? resources.shown)?.leaders ?? []
  const listed = leader !== null && free.some(({ id }) => id === leader.id)
  const lost = leader !== null && resources.data !== null && !listed
  // A chosen leader that is no longer free stays listed, so the choice remains visible
  const options = leader && !listed ? [leader, ...free] : free

  let hint = ''
  if (!resources.wanted) hint = 'Choisissez d’abord la date et l’heure de départ.'
  else if (resources.error) hint = resources.error
  else if (!resources.data) hint = 'Vérification des disponibilités…'
  else if (lost) hint = 'Indisponible à cet horaire.'
  else if (free.length === 0) hint = 'Aucun chef d’équipe libre à cet horaire.'

  return (
    <div className="field">
      <label htmlFor="team-leader">Chef d’équipe</label>
      <select
        id="team-leader" value={leader?.id ?? ''} disabled={!resources.data} aria-describedby="team-leader-hint" aria-invalid={lost || undefined}
        onChange={(event) => onChange(options.find(({ id }) => id === event.target.value))}
      >
        <option value="" disabled>Choisir</option>
        {options.map(({ id, fullName }) => <option key={id} value={id}>{fullName}{lost && id === leader.id ? ' (indisponible)' : ''}</option>)}
      </select>
      <span id="team-leader-hint" role="status" className={lost || resources.error ? 'field-hint urgent' : 'field-hint muted'}>{hint}</span>
      {resources.error && <button className="button-quiet" type="button" onClick={resources.retry}>Réessayer</button>}
    </div>
  )
}
