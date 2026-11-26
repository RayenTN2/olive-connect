import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { getHarvestRequest, reviewHarvestRequest } from '../api/planning.js'
import { loadErrorMessage } from '../auth/messages.js'
import PlanningMap from '../components/map/PlanningMap.jsx'
import OrchardReports from '../components/planning/OrchardReports.jsx'
import { formatDate, formatInstant, formatKg, formatTime, maturityOf, PRIORITIES, REQUEST_STATUSES, REQUEST_TYPES } from '../planning/labels.js'
import { STATE_BY_STATUS } from '../planning/orchards.js'
import { nextPending, readRequestFilters } from '../planning/requests.js'
import { ACTION_NOT_DONE, ACTION_UNVERIFIED, isRequestGone, isUncertain, REQUEST_GONE, requestError } from '../planning/saveErrors.js'
import { invalidate, revalidate } from '../planning/cache.js'
import { loadRequests } from '../planning/loaders.js'
import { rememberItem, useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: facts, notices, loading
import '../components/planning/TourBuilder.css'
import '../components/planning/TourReview.css'
import './ToursPage.css'
import './TourPage.css'

// null = it doesn't exist
const readRequest = (id) => getHarvestRequest(id).catch((failure) => {
  if (isRequestGone(failure.status, failure.message)) return null
  throw failure
})
const loadRequest = rememberItem('request', async (id) => ({ item: await readRequest(id) }))

// The statuses a request can have once accepted or refused
const ACCEPTED = ['APPROVED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED']

function Fact({ term, children }) {
  return <div><dt>{term}</dt><dd>{children}</dd></div>
}

function Decision({ busy, onDecide }) {
  const [asking, setAsking] = useState(null) // null, 'accept' or 'refuse'
  const [comment, setComment] = useState('')
  const [missing, setMissing] = useState(false)
  const fieldRef = useRef(null)

  function decide() {
    if (busy) return
    const text = comment.trim()
    setMissing(asking === 'refuse' && !text)
    if (asking === 'refuse' && !text) fieldRef.current.focus()
    else onDecide(asking === 'accept', text)
  }

  if (!asking) {
    return (
      <div className="foot-pair">
        <button className="button" type="button" onClick={() => setAsking('accept')}>Accepter</button>
        <button className="button-quiet danger" type="button" onClick={() => { setMissing(false); setAsking('refuse') }}>Refuser</button>
      </div>
    )
  }
  const accepting = asking === 'accept'
  return (
    <div className="leaving" role="group" aria-label={accepting ? 'Accepter la demande' : 'Refuser la demande'}>
      <p>{accepting ? 'Le verger pourra être planifié.' : 'Le verger ne pourra pas être planifié.'} Cette décision ne peut pas être modifiée.</p>
      <label className="field">
        {accepting ? 'Commentaire (facultatif)' : 'Motif du refus'}
        <textarea ref={fieldRef} rows={3} maxLength={500} value={comment} autoFocus aria-invalid={missing || undefined} aria-describedby="decision-hint" onChange={(event) => { setComment(event.target.value); setMissing(false) }} />
      </label>
      <p id="decision-hint" className={missing ? 'field-hint urgent' : 'field-hint muted'} role={missing ? 'alert' : undefined}>
        {missing ? 'Indiquez le motif : l’agriculteur doit savoir pourquoi.' : 'L’agriculteur pourra lire ce texte.'}
      </p>
      <button className={accepting ? 'button-quiet' : 'button-quiet danger'} type="button" aria-disabled={busy} onClick={decide}>
        {busy ? 'Enregistrement…' : accepting ? 'Accepter la demande' : 'Refuser la demande'}
      </button>
      <button className="button-quiet" type="button" aria-disabled={busy} onClick={() => !busy && setAsking(null)}>Retour</button>
    </div>
  )
}

function Request({ id, query }) {
  const loaded = useLoad(id, loadRequest)
  const [current, setCurrent] = useState(null) // { item, notice }: newer than what was loaded
  const [request, setRequest] = useState({}) // { busy, error }
  const busyRef = useRef(false) // blocks a second click before React has redrawn
  const titleRef = useRef(null)
  const item = current ? current.item : loaded.data?.item
  const status = item?.status
  // Asked only once this one is decided
  const others = useLoad(item && status !== 'PENDING' ? 'others' : null, loadRequests)
  const next = others.data && nextPending(others.data, readRequestFilters(new URLSearchParams(query)), id)

  const row = useMemo(() => item && {
    id: item.orchardId, name: item.orchardName ?? 'Verger inconnu', state: STATE_BY_STATUS[item.status] ?? 'none', maturity: item.orchardMaturity, harvest: item,
    position: item.latitude != null && item.longitude != null ? [item.latitude, item.longitude] : null,
  }, [item])

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
      const read = await readRequest(id)
      if (!read) return REQUEST_GONE
      setCurrent({ item: read, notice: '' })
      others.retry()
      return ''
    } catch (failure) {
      return loadErrorMessage(failure.status)
    }
  })

  function changed() {
    invalidate('requests', 'orchards')
    revalidate(`request|${id}`)
  }

  const decide = (accepted, comment) => run(async () => {
    try {
      setCurrent({ item: await reviewHarvestRequest(id, accepted, comment), notice: accepted ? 'Le verger peut être planifié.' : 'Le refus est enregistré.' })
      changed()
      return ''
    } catch (failure) {
      const uncertain = isUncertain(failure.status)
      if (uncertain || /^Only PENDING/.test(failure.message)) changed()
      if (!uncertain && !/^Only PENDING/.test(failure.message)) return requestError(failure.status, failure.message)
      // null = gone, undefined = could not be read
      const read = await readRequest(id).catch(() => undefined)
      if (read === undefined) return uncertain ? ACTION_UNVERIFIED : requestError(failure.status, failure.message)
      if (read === null) return REQUEST_GONE
      const done = accepted ? ACCEPTED.includes(read.status) : read.status === 'REJECTED'
      setCurrent({ item: read, notice: uncertain && done ? 'La décision est enregistrée.' : '' })
      if (!uncertain) return requestError(failure.status, failure.message)
      if (done) return ''
      return read.status === 'PENDING' ? ACTION_NOT_DONE : requestError(400, 'Only PENDING')
    }
  })

  const back = <Link className="button-quiet back" to={`/demandes${query}`}>Toutes les demandes</Link>
  if (!item) {
    return (
      <main className="page">
        {back}
        {loaded.error && <div className="notice" role="alert"><p>{loaded.error}</p><button className="button-quiet" type="button" onClick={loaded.retry}>Réessayer</button></div>}
        {!loaded.error && !loaded.data && <div className="loading" role="status"><span className="visually-hidden">Chargement de la demande</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
        {loaded.data && <h1 className="tour-gone">{REQUEST_GONE}</h1>}
      </main>
    )
  }
  const maturity = maturityOf(item.orchardMaturity)
  return (
    <main className="tour-page detail-page">
      <aside className="builder review" aria-label="Demande de récolte">
        <div className="builder-head">
          <div>
            {back}
            <h1 ref={titleRef} tabIndex={-1}>{row.name}</h1>
            <p className="muted" role="status"><strong className={`tour-status ${status}`}>{REQUEST_STATUSES[status] ?? status}</strong> {current?.notice}</p>
          </div>
          <button className="button-quiet" type="button" aria-disabled={Boolean(request.busy)} onClick={refresh}>Actualiser</button>
        </div>
        <div className="builder-foot">
          {request.error && <p className="urgent" role="alert">{request.error}</p>}
          {status === 'PENDING' && <Decision busy={Boolean(request.busy)} onDecide={decide} />}
          {status !== 'PENDING' && (
            <div className="foot-pair links">
              {status === 'APPROVED' && <Link className="button" to={`/planification?verger=${encodeURIComponent(item.orchardId)}`}>Planifier ce verger</Link>}
              {next && <Link className="button-quiet" to={`/demandes/${encodeURIComponent(next.id)}${query}`}>Demande suivante : {next.orchardName ?? 'verger inconnu'}</Link>}
              {others.data && !next && <p className="muted">Aucune autre demande n’attend de décision avec ces filtres.</p>}
            </div>
          )}
        </div>
        <div className="builder-body panel-body">
          {item.reviewedAt && (
            <>
              <h2>Décision</h2>
              <dl className="facts"><Fact term="Prise le">{formatInstant(item.reviewedAt)}, {formatTime(item.reviewedAt)}</Fact></dl>
              {item.reviewComment && <p className="note">{item.reviewComment}</p>}
            </>
          )}
          <h2>Demande</h2>
          <dl className="facts">
            <Fact term="Type">{item.requestType === 'URGENT' ? <strong className="urgent">{REQUEST_TYPES.URGENT}</strong> : REQUEST_TYPES[item.requestType]}</Fact>
            <Fact term="Quantité estimée">{formatKg(item.estimatedQuantityKg)}</Fact>
            <Fact term="Date souhaitée">{item.preferredDate ? formatDate(item.preferredDate) : 'Non renseignée'}</Fact>
            <Fact term="Reçue le">{item.submittedAt ? formatInstant(item.submittedAt) : 'Non renseignée'}</Fact>
          </dl>
          {item.notes && <p className="note">{item.notes}</p>}
          <h2>Priorité</h2>
          <dl className="facts">
            <Fact term="Niveau">{PRIORITIES[item.priorityTier] ?? 'Non calculée'}</Fact>
            {item.priorityScore != null && <Fact term="Score">{item.priorityScore} sur 90</Fact>}
            <Fact term="Maturité du verger"><span className="maturity"><i style={{ background: maturity.color }} />{maturity.label}</span></Fact>
          </dl>
          <p className="muted review-note">Le score vient du type de demande (urgente ou planifiée) et de la maturité du verger. Vous restez libre de la décision.</p>
          <h2>Agriculteur</h2>
          <dl className="facts">
            <Fact term="Nom">{item.farmerName ?? 'Non renseigné'}</Fact>
            <Fact term="Téléphone">{item.farmerPhone ? <a href={`tel:${item.farmerPhone}`}>{item.farmerPhone}</a> : 'Non renseigné'}</Fact>
          </dl>
          {!row.position && <p className="notice">Ce verger n’a pas de coordonnées : il n’apparaît pas sur la carte et ne peut pas être ajouté à une tournée.</p>}
          <OrchardReports key={item.orchardId} orchardId={item.orchardId} />
        </div>
      </aside>
      <PlanningMap
        single rows={[row]} stopIds={[]} mills={[]} tourMillIds={[]} route={null} focusTarget={row} selectedId={null} selectedMillId={null}
        onSelect={() => {}} onSelectMill={() => {}} onHover={() => {}}
      />
    </main>
  )
}

export default function RequestPage() {
  const { id } = useParams()
  const { search } = useLocation()
  return <Request key={id} id={id} query={search} />
}
