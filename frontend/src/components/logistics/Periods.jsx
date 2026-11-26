import { useState } from 'react'
import { createPeriod, deletePeriod, getPeriods, updatePeriod } from '../../api/logistics.js'
import { STATUSES, toInstant, toLocal } from '../../logistics/calendar.js'
import { checkPeriod } from '../../logistics/forms.js'
import { loadPeriods } from '../../logistics/loaders.js'
import { saveChange, saveNew, saveRemoval } from '../../logistics/save.js'
import { useRun } from '../../logistics/useRun.js'
import { addDays } from '../../planning/dashboard.js'
import { formatInstant, formatTime } from '../../planning/labels.js'
import { tunisDay } from '../../planning/tours.js'
import { useLoad } from '../../planning/useLoad.js'
import { when } from './names.js'
import { Ask, Field, FormButtons, LoadError, Skeleton } from './parts.jsx'
import { useForm } from './useForm.jsx'

function PeriodForm({ kind, id, period, now, run, onSaved, onCancel, onChangedElsewhere }) {
  const equipment = kind === 'equipment'
  const tomorrow = addDays(tunisDay(now), 1)
  const { values, errors, set, accept } = useForm(period
    ? { start: toLocal(period.startTime), end: toLocal(period.endTime), reason: period.reason, status: period.status }
    : { start: `${tomorrow}T07:00`, end: `${tomorrow}T17:00`, reason: '', status: 'UNDER_MAINTENANCE' })

  const send = () => run.run(async () => {
    const start = toInstant(values.start).getTime()
    const end = toInstant(values.end).getTime()
    const body = { startTime: new Date(start).toISOString(), endTime: new Date(end).toISOString(), reason: values.reason.trim(), ...(equipment && { status: values.status }) }
    const list = () => getPeriods(kind, id)
    const matches = (item) => Date.parse(item.startTime) === start && Date.parse(item.endTime) === end && item.reason === body.reason && (!equipment || item.status === body.status)
    try {
      if (period) await saveChange({ base: period, read: async () => (await list()).find((item) => item.id === period.id) ?? null, send: () => updatePeriod(kind, id, period.id, body), matches })
      else await saveNew({ list, send: () => createPeriod(kind, id, body), matches })
    } catch (failure) {
      if (failure.current) onChangedElsewhere()
      throw failure
    }
    onSaved(period ? 'La période est modifiée.' : 'La période est ajoutée.')
  })

  function submit(event) {
    event.preventDefault()
    if (accept(checkPeriod(values), event.currentTarget)) send()
  }

  return (
    <form className="period-form" onSubmit={submit} noValidate aria-label={period ? 'Modifier la période' : 'Nouvelle période'}>
      <div className="field-pair">
        <Field label="Début" error={errors.start}>{(props) => <input type="datetime-local" max="9999-12-31T23:59" autoFocus {...props} value={values.start} onChange={(event) => set({ start: event.target.value })} />}</Field>
        <Field label="Fin" error={errors.end}>{(props) => <input type="datetime-local" max="9999-12-31T23:59" {...props} value={values.end} onChange={(event) => set({ end: event.target.value })} />}</Field>
      </div>
      {equipment && (
        <Field label="Nature">{(props) => (
          <select {...props} value={values.status} onChange={(event) => set({ status: event.target.value })}>
            <option value="UNDER_MAINTENANCE">En maintenance</option>
            <option value="UNAVAILABLE">Indisponible</option>
          </select>
        )}</Field>
      )}
      <Field label="Motif" hint="Début et fin à l’heure de Tunis." error={errors.reason}>{(props) => <textarea rows={2} {...props} value={values.reason} onChange={(event) => set({ reason: event.target.value })} />}</Field>
      {run.error && <p className="urgent" role="alert">{run.error}</p>}
      <FormButtons busy={run.busy} label={period ? 'Enregistrer' : 'Ajouter'} onCancel={onCancel} />
    </form>
  )
}

function Period({ period, now, busy, onEdit, onDelete }) {
  const [asking, setAsking] = useState(false)
  return (
    <li>
      <div className="period">
        <span className="period-main">
          <span className="period-dates">{when(period.startTime)} – {formatInstant(period.startTime) === formatInstant(period.endTime) ? formatTime(period.endTime) : when(period.endTime)}</span>
          <span className="muted">{period.reason}</span>
        </span>
        {period.status && <span className={`tour-status ${period.status}`}>{STATUSES[period.status]}</span>}
        {Date.parse(period.startTime) <= now && Date.parse(period.endTime) > now && <span className="tour-status PLANNED">En cours</span>}
      </div>
      {asking ? (
        <Ask label="Supprimer la période" danger busy={busy} onConfirm={onDelete} onCancel={() => setAsking(false)}><p>La période sera supprimée définitivement.</p></Ask>
      ) : (
        <div className="period-actions">
          <button className="button-quiet" type="button" onClick={onEdit}>Modifier</button>
          <button className="button-quiet danger" type="button" onClick={() => setAsking(true)}>Supprimer</button>
        </div>
      )}
    </li>
  )
}

// kind: 'workers' | 'equipment'
export default function Periods({ kind, id, now, onChanged }) {
  const periods = useLoad(`${kind}|${id}`, loadPeriods)
  const [editing, setEditing] = useState(null) // null | 'new' | period id
  const [notice, setNotice] = useState('')
  const [showPast, setShowPast] = useState(false)
  const run = useRun()
  const list = periods.data ?? []
  const current = list.filter(({ endTime }) => Date.parse(endTime) > now)
  const past = list.filter(({ endTime }) => Date.parse(endTime) <= now).reverse()

  function open(value) {
    if (run.busy) return
    run.clearError()
    setNotice('')
    setEditing(value)
  }
  function saved(text) {
    setEditing(null)
    setNotice(text)
    onChanged()
  }
  const remove = (period) => run.run(async () => {
    await saveRemoval({ list: () => getPeriods(kind, id), send: () => deletePeriod(kind, id, period.id), id: period.id })
    saved('La période est supprimée.')
  })
  const form = (period) => <PeriodForm key={period?.updatedAt} kind={kind} id={id} period={period} now={now} run={run} onSaved={saved} onCancel={() => open(null)} onChangedElsewhere={onChanged} />
  const row = (period) => (editing === period.id
    ? <li key={period.id}>{form(period)}</li>
    : <Period key={period.id} period={period} now={now} busy={run.busy} onEdit={() => open(period.id)} onDelete={() => remove(period)} />)

  return (
    <section className="block" aria-label="Indisponibilités">
      <div className="block-head">
        <h2>Indisponibilités</h2>
        {periods.data && editing !== 'new' && <button className="button-quiet" type="button" onClick={() => open('new')}>Ajouter une période</button>}
      </div>
      <p className="period-notice" role="status">{notice}</p>
      {editing === 'new' && form(null)}
      {run.error && editing === null && <p className="urgent" role="alert">{run.error}</p>}
      {periods.error && <LoadError load={periods} />}
      {!periods.error && !periods.data && <Skeleton label="Chargement des périodes" />}
      {periods.data && current.length === 0 && editing !== 'new' && <p className="muted">Aucune période en cours ou à venir.</p>}
      <ul className="periods">{current.map(row)}</ul>
      {past.length > 0 && <button className="button-quiet past-toggle" type="button" aria-expanded={showPast} onClick={() => setShowPast(!showPast)}>{showPast ? 'Masquer' : 'Voir'} les périodes passées ({past.length})</button>}
      {showPast && <ul className="periods">{past.map(row)}</ul>}
    </section>
  )
}
