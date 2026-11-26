import { useEffect, useRef, useState } from 'react'
import { formatKg } from '../../planning/labels.js'
import { totalKg } from '../../planning/tour.js'
import ConstraintList from './ConstraintList.jsx'
import DropOffEditor from './DropOffEditor.jsx'
import ResourcePicker from './ResourcePicker.jsx'
import StopList from './StopList.jsx'
import TeamLeaderField from './TeamLeaderField.jsx'
import './TourBuilder.css'

const SECTIONS = { stops: 'Arrêts', organisation: 'Organisation', resources: 'Ressources' }

export default function TourBuilder({ tour, stops, dispatch, rules, mills, booked, resources, onShow, onHoverMill, onSave, saving, error, editing, onDiscard }) {
  const [section, setSection] = useState('stops')
  const [leaving, setLeaving] = useState(false)
  const [incomplete, setIncomplete] = useState(false)
  // Closed at first, so a new tour doesn't open on a list of everything still missing
  const [checksOpen, setChecksOpen] = useState(false)
  const titleRef = useRef(null)
  const firstLeft = rules.find(({ status }) => status === 'todo')
  const set = (field) => (event) => dispatch({ type: 'set', field, value: event.target.value })

  // Opened from a saved draft: the review that held the keyboard is gone
  useEffect(() => { if (editing) titleRef.current.focus() }, [editing])

  async function save() {
    if (saving) return
    setIncomplete(Boolean(firstLeft))
    if (firstLeft) setChecksOpen(true)
    // onSave answers with the section the server's refusal is about, if any
    const target = firstLeft ? firstLeft.section : await onSave()
    if (target) setSection(target)
  }

  const message = incomplete && firstLeft ? `${firstLeft.label} : ${firstLeft.detail}` : error?.text
  return (
    <aside className="builder float-right" aria-label="Tournée en préparation">
      <div className="builder-head">
        <div>
          <h2 ref={titleRef} tabIndex={-1}>{editing ? 'Modifier le brouillon' : 'Nouvelle tournée'}</h2>
          <p className="muted">{stops.length} {stops.length > 1 ? 'arrêts' : 'arrêt'}, {formatKg(totalKg(stops))}</p>
        </div>
        {!leaving && !saving && <button className="button-quiet" type="button" onClick={() => setLeaving(true)}>Abandonner</button>}
      </div>
      {leaving && !saving && (
        <div className="leaving" role="alert">
          <p>{editing ? 'Vos modifications seront perdues. Le brouillon enregistré est conservé.' : 'Cette tournée n’est pas enregistrée. Tout sera perdu.'}</p>
          <button className="button-quiet danger" type="button" onClick={onDiscard}>{editing ? 'Abandonner les modifications' : 'Abandonner la tournée'}</button>
          <button className="button-quiet" type="button" autoFocus onClick={() => setLeaving(false)}>Continuer</button>
        </div>
      )}
      <nav className="builder-nav" aria-label="Sections de la tournée">
        {Object.entries(SECTIONS).map(([key, label]) => {
          const left = rules.filter((rule) => rule.section === key && rule.status === 'todo').length
          return (
            <button key={key} type="button" aria-current={section === key ? 'true' : undefined} onClick={() => setSection(key)}>
              {label}{left > 0 && <span className="count" aria-label={`${left} à compléter`}>{left}</span>}
            </button>
          )
        })}
      </nav>
      <div className="builder-body" inert={saving}>
        {section === 'stops' && <StopList stops={stops} dispatch={dispatch} onShow={onShow} />}
        {section === 'organisation' && (
          <>
            <label className="field">Nom de la tournée<input value={tour.title} maxLength={150} onChange={set('title')} /></label>
            <div className="field-pair">
              <label className="field">Date<input type="date" value={tour.date} onChange={set('date')} /></label>
              <label className="field">Départ (heure de Tunis)<input type="time" min="07:00" max="17:00" value={tour.time} onChange={set('time')} /></label>
            </div>
            <TeamLeaderField leader={tour.leader} resources={resources} onChange={(value) => dispatch({ type: 'set', field: 'leader', value })} />
            <DropOffEditor dropOffs={tour.dropOffs} quantity={totalKg(stops)} mills={mills} booked={booked} dispatch={dispatch} onHover={onHoverMill} />
            <label className="field">Notes (facultatif)<textarea rows={3} maxLength={500} value={tour.notes} onChange={set('notes')} /></label>
          </>
        )}
        {section === 'resources' && (
          <ResourcePicker tour={tour} stops={stops} resources={resources} dispatch={dispatch} onSchedule={() => setSection('organisation')} />
        )}
      </div>
      <ConstraintList rules={rules} open={checksOpen} onToggle={() => setChecksOpen(!checksOpen)} onOpen={setSection} />
      <div className="builder-foot">
        {message && !saving && <p className="urgent" role="alert">{message}</p>}
        <button className="button" type="button" aria-disabled={saving} onClick={save}>
          {saving && <span className="spinner" aria-hidden="true" />}{saving ? 'Enregistrement…' : editing ? 'Enregistrer les modifications' : 'Enregistrer le brouillon'}
        </button>
      </div>
    </aside>
  )
}
