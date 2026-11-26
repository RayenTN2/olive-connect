import { useRef, useState } from 'react'
import { equipmentTypeLabel, formatDate, formatKg } from '../planning/labels.js'
import { normalize } from '../planning/orchards.js'
import { periodWindow, readPeriod, readTab, resourceTabs } from '../planning/resources.js'
import { binCapacityKg } from '../planning/tour.js'
import { tunisDay } from '../planning/tours.js'
import { useAddress } from '../planning/useAddress.js'
import { useFree } from '../planning/useFree.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'
import './ResourcesPage.css'

const NONE = { chefs: 'Aucun chef d’équipe libre.', ouvriers: 'Aucun ouvrier libre.' }

// The kinds of resources, always all visible; arrows move between them
function Tabs({ tabs, current, onChange }) {
  const listRef = useRef(null)
  function move(event) {
    const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key]
    if (!step) return
    event.preventDefault()
    const next = tabs[(tabs.findIndex(({ key }) => key === current) + step + tabs.length) % tabs.length]
    onChange(next.key)
    requestAnimationFrame(() => listRef.current.querySelector(`[data-key="${next.key}"]`)?.focus())
  }
  return (
    <div className="resource-tabs" role="tablist" aria-label="Type de ressource" ref={listRef} onKeyDown={move}>
      {tabs.map(({ key, label, count, capacity }) => (
        <button
          key={key} data-key={key} type="button" role="tab" id={`tab-${key}`} aria-controls="resource-list" aria-selected={key === current}
          tabIndex={key === current ? 0 : -1} className={count === 0 ? 'none' : undefined} onClick={() => onChange(key)}
        >
          <span>{label}</span>
          <span className="tab-count">{count}</span>
          {capacity != null && <span className="tab-extra">{formatKg(capacity)}</span>}
        </button>
      ))}
    </div>
  )
}

export default function ResourcesPage() {
  const { params, text, type, change } = useAddress()
  const [today] = useState(() => tunisDay(Date.now()))
  const period = readPeriod(params, today)
  const span = periodWindow(period)
  const free = useFree(span)
  const set = (key) => (event) => event.target.value && change({ [key]: event.target.value }, true)

  const data = free.error ? null : free.shown
  const tabs = data && resourceTabs(data, equipmentTypeLabel, binCapacityKg)
  const tab = tabs && tabs.find(({ key }) => key === readTab(params, tabs))
  const needle = normalize(text.trim())
  const shown = tab && tab.items.filter(({ name, detail, phone }) => !needle || [name, detail, phone].some((value) => value && normalize(value).includes(needle)))
  return (
    <main className="page">
      <div className="page-head">
        <h1>Ressources libres</h1>
        <button className="button-quiet" type="button" onClick={free.retry}>Actualiser</button>
      </div>
      <div className="tour-tools period">
        <label>Jour <input type="date" value={period.jour} onChange={set('jour')} /></label>
        <label>de <input type="time" value={period.de} aria-invalid={!span || undefined} onChange={set('de')} /></label>
        <label>à <input type="time" value={period.a} aria-invalid={!span || undefined} onChange={set('a')} /></label>
        <span className="muted">Heure de Tunis</span>
        <span className="muted" role="status">{span && free.checking ? 'Vérification…' : ''}</span>
      </div>
      {!span ? <p className="notice" role="alert">L’heure de fin doit être après l’heure de début.</p> : (
        <>
          <p className="muted">Libres pendant toute la période, le {formatDate(period.jour)}. Seules les tournées confirmées ou en cours et les indisponibilités enregistrées comptent.</p>
          {free.error && <div className="notice" role="alert"><p>{free.error}</p><button className="button-quiet" type="button" onClick={free.retry}>Réessayer</button></div>}
          {!free.error && !tabs && <div className="loading" role="status"><span className="visually-hidden">Vérification des disponibilités</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
          {tabs && (
            <div className={free.known ? undefined : 'checking'} aria-busy={free.checking}>
              <Tabs tabs={tabs} current={tab.key} onChange={(key) => change({ voir: key, q: '' })} />
              <section className="resource-panel" id="resource-list" role="tabpanel" aria-labelledby={`tab-${tab.key}`}>
                <div className="resource-tools">
                  <input type="search" aria-label="Rechercher dans la liste" placeholder={tab.key === 'ouvriers' ? 'Nom, téléphone' : 'Nom, référence'} value={text} onChange={(event) => type(event.target.value)} />
                  <p className="muted" role="status">{needle ? `${shown.length} sur ${tab.count}` : `${tab.count} ${tab.label.toLowerCase()}`}</p>
                </div>
                {tab.count === 0 && <p className="muted">{NONE[tab.key] ?? 'Aucun équipement de ce type n’est libre.'}</p>}
                {tab.count > 0 && shown.length === 0 && <p className="muted">Aucun résultat pour « {text.trim()} ».</p>}
                {shown.length > 0 && (
                  <ul className="resource-list">
                    {shown.map(({ id, name, detail, phone }) => (
                      <li key={id}><span>{name}</span>{phone ? <a className="muted" href={`tel:${phone}`}>{phone}</a> : detail && <span className="muted">{detail}</span>}</li>
                    ))}
                  </ul>
                )}
              </section>
            </div>
          )}
        </>
      )}
    </main>
  )
}
