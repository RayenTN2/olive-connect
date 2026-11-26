import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { getProblem, updateProblemStatus } from '../api/planning.js'
import { loadErrorMessage } from '../auth/messages.js'
import PlanningMap from '../components/map/PlanningMap.jsx'
import OrchardReports from '../components/planning/OrchardReports.jsx'
import { formatInstant, formatTime, PROBLEM_STATUSES, PROBLEMS } from '../planning/labels.js'
import { nextStatuses } from '../planning/reports.js'
import { ACTION_NOT_DONE, ACTION_UNVERIFIED, isProblemGone, isUncertain, PROBLEM_GONE, problemError } from '../planning/saveErrors.js'
import { invalidate, revalidate } from '../planning/cache.js'
import { rememberItem, useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: facts, notices, loading
import '../components/planning/TourBuilder.css'
import '../components/planning/TourReview.css'
import './ToursPage.css'
import './TourPage.css'

// null = it doesn't exist
const readReport = (id) => getProblem(id).catch((failure) => {
  if (isProblemGone(failure.status, failure.message)) return null
  throw failure
})
const loadReport = rememberItem('problem', async (id) => ({ report: await readReport(id) }))

const CHANGES = {
  UNDER_REVIEW: { label: 'Commencer l’examen', done: 'L’examen est commencé.' },
  RESOLVED: { label: 'Résolu', confirm: 'Marquer comme résolu', text: 'Le problème sera marqué comme résolu.', done: 'Le problème est résolu.' },
  REJECTED: { label: 'Rejeter', confirm: 'Rejeter le signalement', text: 'Le signalement sera rejeté.', done: 'Le signalement est rejeté.' },
}

function StatusChanges({ status, busy, onChange }) {
  const [asking, setAsking] = useState(null)
  if (asking) {
    const { confirm, text } = CHANGES[asking]
    return (
      <div className="leaving" role="group" aria-label={confirm}>
        <p>{text} Il restera consultable, mais ne pourra pas être rouvert ici.</p>
        <button className={asking === 'REJECTED' ? 'button-quiet danger' : 'button-quiet'} type="button" aria-disabled={busy} onClick={() => !busy && onChange(asking)}>{busy ? 'Enregistrement…' : confirm}</button>
        <button className="button-quiet" type="button" autoFocus aria-disabled={busy} onClick={() => !busy && setAsking(null)}>Retour</button>
      </div>
    )
  }
  return (
    <div className="foot-pair changes">
      {nextStatuses(status).map((next, index) => (
        <button
          key={next} type="button" aria-disabled={busy} className={index === 0 ? 'button' : next === 'REJECTED' ? 'button-quiet danger' : 'button-quiet'}
          onClick={() => { if (!busy) { if (CHANGES[next].confirm) setAsking(next); else onChange(next) } }}
        >
          {busy && index === 0 ? 'Enregistrement…' : CHANGES[next].label}
        </button>
      ))}
    </div>
  )
}

function Report({ id, query }) {
  const loaded = useLoad(id, loadReport)
  const [current, setCurrent] = useState(null) // { report, notice }: newer than what was loaded
  const [request, setRequest] = useState({}) // { busy, error }
  const busyRef = useRef(false) // blocks a second click before React has redrawn
  const titleRef = useRef(null)
  const report = current ? current.report : loaded.data?.report
  const status = report?.status

  const row = useMemo(() => report && {
    id: report.orchardId, name: report.orchardName ?? 'Verger inconnu', state: 'place', harvest: null,
    position: report.latitude != null && report.longitude != null ? [report.latitude, report.longitude] : null,
  }, [report])

  // Each new state takes the keyboard: the button that held it may be gone
  useEffect(() => titleRef.current?.focus(), [status])

  async function run(work) {
    if (busyRef.current) return
    busyRef.current = true
    setRequest({ busy: true })
    try {
      setRequest({ error: await work() })
    } finally {
      busyRef.current = false
    }
  }

  const refresh = () => run(async () => {
    try {
      const read = await readReport(id)
      if (!read) return PROBLEM_GONE
      setCurrent({ report: read, notice: '' })
      return ''
    } catch (failure) {
      return loadErrorMessage(failure.status)
    }
  })

  function changed() {
    invalidate('problems')
    revalidate(`problem|${id}`)
  }

  const change = (next) => run(async () => {
    // The backend doesn't check the previous status: look first, so another manager's decision isn't replaced
    try {
      const before = await readReport(id)
      if (!before) return PROBLEM_GONE
      if (before.status !== status) {
        setCurrent({ report: before, notice: '' })
        changed()
        return 'Ce signalement a été traité entre-temps. Vérifiez le statut affiché.'
      }
    } catch (failure) {
      return loadErrorMessage(failure.status)
    }
    try {
      setCurrent({ report: await updateProblemStatus(id, next), notice: CHANGES[next].done })
      changed()
      return ''
    } catch (failure) {
      if (!isUncertain(failure.status)) return problemError(failure.status, failure.message)
      changed()
      // null = gone, undefined = could not be read
      const read = await readReport(id).catch(() => undefined)
      if (read === undefined) return ACTION_UNVERIFIED
      if (read === null) return PROBLEM_GONE
      setCurrent({ report: read, notice: read.status === next ? CHANGES[next].done : '' })
      return read.status === next ? '' : ACTION_NOT_DONE
    }
  })

  const back = <Link className="button-quiet back" to={`/signalements${query}`}>Tous les signalements</Link>
  if (!report) {
    return (
      <main className="page">
        {back}
        {loaded.error && <div className="notice" role="alert"><p>{loaded.error}</p><button className="button-quiet" type="button" onClick={loaded.retry}>Réessayer</button></div>}
        {!loaded.error && !loaded.data && <div className="loading" role="status"><span className="visually-hidden">Chargement du signalement</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
        {loaded.data && <h1 className="tour-gone">{PROBLEM_GONE}</h1>}
      </main>
    )
  }
  return (
    <main className="tour-page detail-page">
      <aside className="builder review" aria-label="Signalement">
        <div className="builder-head">
          <div>
            {back}
            <h1 ref={titleRef} tabIndex={-1}>{PROBLEMS[report.problemType] ?? PROBLEMS.OTHER}</h1>
            <p className="muted" role="status"><strong className={`tour-status ${status}`}>{PROBLEM_STATUSES[status] ?? status}</strong> {current?.notice}</p>
          </div>
          <button className="button-quiet" type="button" aria-disabled={Boolean(request.busy)} onClick={refresh}>Actualiser</button>
        </div>
        {(request.error || nextStatuses(status).length > 0) && (
          <div className="builder-foot">
            {request.error && <p className="urgent" role="alert">{request.error}</p>}
            {nextStatuses(status).length > 0 && <StatusChanges key={status} status={status} busy={Boolean(request.busy)} onChange={change} />}
          </div>
        )}
        <div className="builder-body panel-body">
          <h2>Problème</h2>
          <dl className="facts">
            <div><dt>Verger</dt><dd><Link to={`/planification?verger=${encodeURIComponent(report.orchardId)}`}>{row.name}</Link></dd></div>
            <div><dt>Reçu le</dt><dd>{report.reportedAt ? `${formatInstant(report.reportedAt)}, ${formatTime(report.reportedAt)}` : 'Non renseigné'}</dd></div>
          </dl>
          {report.description ? <p className="note">{report.description}</p> : <p className="muted review-note">Aucune description.</p>}
          <p className="muted review-note">{row.position ? 'La carte montre le verger : un signalement n’a pas de position propre.' : 'Ce verger n’a pas de coordonnées.'}</p>
          <OrchardReports key={`${report.orchardId} ${status}`} orchardId={report.orchardId} />
        </div>
      </aside>
      <PlanningMap
        single rows={[row]} stopIds={[]} mills={[]} tourMillIds={[]} route={null} focusTarget={row} selectedId={null} selectedMillId={null}
        onSelect={() => {}} onSelectMill={() => {}} onHover={() => {}}
      />
    </main>
  )
}

export default function ReportPage() {
  const { id } = useParams()
  const { search } = useLocation()
  return <Report key={id} id={id} query={search} />
}
