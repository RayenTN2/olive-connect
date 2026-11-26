import { useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router'
import { getResources } from '../../api/logistics.js'
import { LoadError, Skeleton, Status, Title } from '../../components/logistics/parts.jsx'
import Periods from '../../components/logistics/Periods.jsx'
import { ActiveSwitch, ResourceTours } from '../../components/logistics/Resource.jsx'
import WorkerWindow from '../../components/logistics/WorkerWindow.jsx'
import { daysKey, statuses } from '../../logistics/calendar.js'
import { loadCalendar, loadWorkers } from '../../logistics/loaders.js'
import { GONE } from '../../logistics/save.js'
import { resourcesChanged } from '../../logistics/useRun.js'
import { revalidate } from '../../planning/cache.js'
import { tunisDay } from '../../planning/tours.js'
import { useLoad } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: facts, notices, loading
import '../ToursPage.css'
import '../TourPage.css'
import '../DashboardPage.css'
import '../../components/planning/TourBuilder.css'
import './Logistics.css'

const DAYS = 60
const read = (id) => async () => (await getResources('workers')).find((worker) => worker.id === id) ?? null

function Worker({ id, query }) {
  const workers = useLoad('workers', loadWorkers)
  const [now, setNow] = useState(() => Date.now())
  const span = daysKey(tunisDay(now), DAYS)
  const calendar = useLoad(span, loadCalendar)
  const [newer, setNewer] = useState(null) // what was saved here, until the list is read again
  const [editing, setEditing] = useState(false)
  const [notice, setNotice] = useState('')
  const listed = workers.data?.find((worker) => worker.id === id)
  const worker = newer && !(Date.parse(listed?.updatedAt) >= Date.parse(newer.updatedAt)) ? newer : listed

  function changed(saved, text) {
    setNow(Date.now())
    if (saved) setNewer(saved)
    setNotice(text)
    resourcesChanged('workers', id, span)
  }
  function refresh() {
    setNow(Date.now())
    setNotice('')
    workers.retry()
    calendar.retry()
    revalidate(`periods|workers|${id}`)
  }

  const back = <Link className="button-quiet back" to={`/logistique/ouvriers${query}`}>Tous les ouvriers</Link>
  if (!worker) {
    return (
      <main className="page">
        {back}
        {workers.error && <LoadError load={workers} />}
        {!workers.error && !workers.checked && <Skeleton label="Chargement de l’ouvrier" />}
        {workers.data && workers.checked && <h1 className="tour-gone">{GONE}</h1>}
      </main>
    )
  }
  return (
    <main className="page">
      {back}
      <div className="resource-head">
        <Title>{worker.fullName}</Title>
        <Status status={statuses([worker], calendar.data, now)[id]} />
        <button className="button-quiet" type="button" onClick={refresh}>Actualiser</button>
      </div>
      {editing && (
        <WorkerWindow
          worker={worker} read={read(id)} onClose={() => setEditing(false)} onChangedElsewhere={(current) => changed(current, '')}
          onSaved={(saved) => {
            setEditing(false)
            changed(saved, 'Les informations sont enregistrées.')
          }}
        />
      )}
      <p className="resource-notice" role="status">{notice}</p>
      <div className="dash">
        <div className="dash-column">
          <section className="block" aria-label="Fiche">
            <div className="block-head"><h2>Fiche</h2></div>
            <dl className="facts">
              <div><dt>Nom complet</dt><dd>{worker.fullName}</dd></div>
              <div><dt>Téléphone</dt><dd>{worker.phone ? <a href={`tel:${worker.phone}`}>{worker.phone}</a> : 'Non renseigné'}</dd></div>
            </dl>
            <div className="resource-actions">
              <button className="button-quiet" type="button" onClick={() => { setNotice(''); setEditing(true) }}>Modifier</button>
              <ActiveSwitch kind="workers" item={worker} read={read(id)} name={worker.fullName} onDone={changed} />
            </div>
          </section>
          <ResourceTours id={id} span={span} days={DAYS} />
        </div>
        <Periods kind="workers" id={id} now={now} onChanged={() => changed(null, '')} />
      </div>
    </main>
  )
}

export default function WorkerPage() {
  const { id } = useParams()
  const { search } = useLocation()
  return id === 'nouveau' ? <Navigate to="/logistique/ouvriers" replace /> : <Worker key={id} id={id} query={search} />
}
