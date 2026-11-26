import { useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router'
import { getEquipmentItem } from '../../api/logistics.js'
import EquipmentWindow from '../../components/logistics/EquipmentWindow.jsx'
import { LoadError, Skeleton, Status, Title } from '../../components/logistics/parts.jsx'
import Periods from '../../components/logistics/Periods.jsx'
import { ActiveSwitch, ResourceTours } from '../../components/logistics/Resource.jsx'
import { daysKey, typeLabel } from '../../logistics/calendar.js'
import { loadCalendar, loadEquipmentItem, loadTypes } from '../../logistics/loaders.js'
import { GONE } from '../../logistics/save.js'
import { resourcesChanged } from '../../logistics/useRun.js'
import { revalidate } from '../../planning/cache.js'
import { formatKg } from '../../planning/labels.js'
import { tunisDay } from '../../planning/tours.js'
import { useLoad } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: facts, notices, loading
import '../ToursPage.css'
import '../TourPage.css'
import '../DashboardPage.css'
import '../../components/planning/TourBuilder.css'
import './Logistics.css'

const DAYS = 60
const read = (id) => () => getEquipmentItem(id).catch((failure) => {
  if (failure.status === 404) return null
  throw failure
})

function Equipment({ id, query }) {
  const loaded = useLoad(id, loadEquipmentItem)
  const types = useLoad('types', loadTypes)
  const [now, setNow] = useState(() => Date.now())
  const span = daysKey(tunisDay(now), DAYS)
  const calendar = useLoad(span, loadCalendar)
  const [newer, setNewer] = useState(null) // what was saved here, until it is read again
  const [editing, setEditing] = useState(false)
  const [notice, setNotice] = useState('')
  const known = loaded.data?.item
  const item = newer && !(Date.parse(known?.updatedAt) >= Date.parse(newer.updatedAt)) ? newer : known

  function changed(saved, text) {
    setNow(Date.now())
    if (saved) setNewer(saved)
    setNotice(text)
    resourcesChanged('equipment', id, span)
  }
  function refresh() {
    setNow(Date.now())
    setNotice('')
    loaded.retry()
    calendar.retry()
    revalidate(`periods|equipment|${id}`)
  }

  const back = <Link className="button-quiet back" to={`/logistique/equipements${query}`}>Tous les équipements</Link>
  if (!item) {
    return (
      <main className="page">
        {back}
        {loaded.error && <LoadError load={loaded} />}
        {!loaded.error && !loaded.data && <Skeleton label="Chargement de l’équipement" />}
        {loaded.data && <h1 className="tour-gone">{GONE}</h1>}
      </main>
    )
  }
  const facts = [
    ['Référence', item.reference], ['Type', typeLabel(item.type, types.data)],
    item.capacityKg != null && ['Capacité', formatKg(item.capacityKg)],
    item.registrationNumber && ['Immatriculation', item.registrationNumber],
    ['Marque', item.brand], ['Modèle', item.model], ['Numéro de série', item.serialNumber],
  ].filter(Boolean)
  const status = !item.active ? 'INACTIVE' : item === known && known.active === loaded.data.status.active ? loaded.data.status.status : 'UNKNOWN'
  return (
    <main className="page">
      {back}
      <div className="resource-head">
        <Title>{item.name || item.reference}</Title>
        <Status status={status} />
        <button className="button-quiet" type="button" onClick={refresh}>Actualiser</button>
      </div>
      {editing && (
        <EquipmentWindow
          item={item} read={read(id)} onClose={() => setEditing(false)} onChangedElsewhere={(current) => changed(current, '')}
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
            <dl className="facts">{facts.map(([term, value]) => <div key={term}><dt>{term}</dt><dd>{value || 'Non renseigné'}</dd></div>)}</dl>
            <div className="resource-actions">
              <button className="button-quiet" type="button" onClick={() => { setNotice(''); setEditing(true) }}>Modifier</button>
              <ActiveSwitch kind="equipment" item={item} read={read(id)} name={item.name || item.reference} onDone={changed} />
            </div>
          </section>
          <ResourceTours id={id} span={span} days={DAYS} />
        </div>
        <Periods kind="equipment" id={id} now={now} onChanged={() => changed(null, '')} />
      </div>
    </main>
  )
}

export default function EquipmentItemPage() {
  const { id } = useParams()
  const { search } = useLocation()
  return id === 'nouveau' ? <Navigate to="/logistique/equipements" replace /> : <Equipment key={id} id={id} query={search} />
}
