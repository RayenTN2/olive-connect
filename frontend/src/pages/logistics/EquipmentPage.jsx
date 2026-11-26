import { useState } from 'react'
import { Link } from 'react-router'
import { createEquipmentType, getEquipmentTypes } from '../../api/logistics.js'
import FilterCounts from '../../components/FilterCounts.jsx'
import { Field, LoadError, Skeleton, Status } from '../../components/logistics/parts.jsx'
import { useForm } from '../../components/logistics/useForm.jsx'
import EquipmentWindow from '../../components/logistics/EquipmentWindow.jsx'
import PageLoading from '../../components/PageLoading.jsx'
import SortableTable from '../../components/SortableTable.jsx'
import { byType, daysKey, statuses, typeLabel } from '../../logistics/calendar.js'
import { readEquipmentFilters, searchEquipment, sortResources } from '../../logistics/lists.js'
import { loadCalendar, loadEquipment, loadTypes } from '../../logistics/loaders.js'
import { saveNew } from '../../logistics/save.js'
import { resourcesChanged, useRun } from '../../logistics/useRun.js'
import { revalidate } from '../../planning/cache.js'
import { formatKg } from '../../planning/labels.js'
import { countByStatus, tunisDay } from '../../planning/tours.js'
import { nextSort, useAddress } from '../../planning/useAddress.js'
import { useLoad, useReady } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: notices, loading
import '../ToursPage.css'
import '../DashboardPage.css'
import '../../components/planning/TourBuilder.css'
import './Logistics.css'

const COLUMNS = [['nom', 'Équipement'], ['reference', 'Référence'], ['type', 'Type'], ['capacite', 'Capacité'], ['statut', 'Situation']]

// Types can only be added: nothing renames or removes one
function Types({ types }) {
  const { values, errors, text, accept, setValues } = useForm({ label: '' })
  const [notice, setNotice] = useState('')
  const run = useRun()

  function submit(event) {
    event.preventDefault()
    const label = values.label.trim()
    setNotice('')
    if (!accept(label ? {} : { label: 'Saisissez le nom du type.' }, event.currentTarget)) return
    run.run(async () => {
      await saveNew({ list: getEquipmentTypes, send: () => createEquipmentType(label), matches: (item) => item.label === label })
      setValues({ label: '' })
      setNotice(`Le type « ${label} » est ajouté.`)
      revalidate('equipment-types')
    })
  }

  return (
    <section className="block type-block" aria-label="Types d’équipement">
      <div className="block-head"><h2>Types d’équipement</h2></div>
      <ul className="types">{types.map(({ code }) => <li key={code}>{typeLabel(code, types)}</li>)}</ul>
      <form className="logistics-form" onSubmit={submit} noValidate>
        <Field label="Nouveau type" hint="Un type ne peut ensuite être ni renommé ni supprimé." error={errors.label}>{text('label')}</Field>
        <button className="button" type="submit" aria-disabled={run.busy}>{run.busy ? 'Ajout…' : 'Ajouter'}</button>
      </form>
      {run.error && <p className="urgent" role="alert">{run.error}</p>}
      <p className="period-notice" role="status">{notice}</p>
    </section>
  )
}

export default function EquipmentPage() {
  const [now, setNow] = useState(() => Date.now())
  const equipment = useLoad('equipment', loadEquipment)
  const types = useLoad('types', loadTypes)
  const span = daysKey(tunisDay(now), 1)
  const calendar = useLoad(span, loadCalendar)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(null)
  const ready = useReady(equipment, types, calendar)
  const { params, text, type, change, clear } = useAddress()
  const filters = readEquipmentFilters(params)
  const query = params.toString()

  if (!ready) return <main className="page"><PageLoading /></main>
  const status = equipment.data && statuses(equipment.data, calendar.data, now)
  const found = equipment.data && searchEquipment(equipment.data, filters)
  const shown = found && sortResources(found.filter(({ id }) => !filters.statut || status[id] === filters.statut), filters, status, types.data)
  const codes = [...new Set([...(types.data ?? []).map(({ code }) => code), ...(equipment.data ?? []).map((item) => item.type)])].sort(byType)
  return (
    <main className="page">
      <div className="page-head">
        <h1>Équipements</h1>
        <button className="button-quiet" type="button" onClick={() => { setNow(Date.now()); for (const load of [equipment, types, calendar]) load.retry() }}>Actualiser</button>
        <button className="button" type="button" onClick={() => { setAdded(null); setAdding(true) }}>Nouvel équipement</button>
      </div>
      <div role="status">{added && <p className="page-notice">« {added.name || added.reference} » est ajouté. <Link to={`/logistique/equipements/${encodeURIComponent(added.id)}`}>Voir la fiche</Link></p>}</div>
      {adding && (
        <EquipmentWindow
          item={null} onClose={() => setAdding(false)}
          onSaved={(saved) => {
            setAdding(false)
            setAdded(saved)
            resourcesChanged('equipment', saved.id, span)
          }}
        />
      )}
      <FilterCounts
        allLabel="Tous" labels={{ AVAILABLE: 'Disponibles', IN_USE: 'En tournée', UNAVAILABLE: 'Indisponibles', UNDER_MAINTENANCE: 'En maintenance', INACTIVE: 'Désactivés' }} total={found?.length}
        counts={found && calendar.data && countByStatus(found.map(({ id }) => ({ status: status[id] })))} value={filters.statut} onChange={(statut) => change({ statut: filters.statut === statut ? '' : statut })}
      />
      <div className="tour-tools">
        <input type="search" aria-label="Rechercher un équipement" placeholder="Nom, référence, marque, série" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Type</span>
          <select value={filters.type} onChange={(event) => change({ type: event.target.value })}>
            <option value="">Tous les types</option>
            {codes.map((code) => <option key={code} value={code}>{typeLabel(code, types.data)}</option>)}
          </select>
        </label>
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'équipements' : 'équipement'}, situation actuelle</p>}
      </div>
      {equipment.error && <LoadError load={equipment} />}
      {calendar.error && !equipment.error && <LoadError load={calendar} before="Les disponibilités n’ont pas pu être vérifiées." />}
      {!equipment.error && !shown && <Skeleton label="Chargement des équipements" />}
      {shown?.length === 0 && (
        <div className="empty">
          <p>{equipment.data.length === 0 ? 'Aucun équipement enregistré.' : 'Aucun équipement ne correspond à ces filtres.'}</p>
          {equipment.data.length > 0 && <button className="button-quiet" type="button" onClick={() => clear()}>Effacer les filtres</button>}
        </div>
      )}
      {shown?.length > 0 && (
        <SortableTable columns={COLUMNS} numeric={['capacite']} filters={filters} onSort={(key) => change(nextSort(filters, key, ['capacite']))}>
          {shown.map((item) => (
            <tr key={item.id}>
              <td><Link to={`/logistique/equipements/${encodeURIComponent(item.id)}${query ? `?${query}` : ''}`}>{item.name || item.reference}</Link></td>
              <td>{item.reference}</td>
              <td>{typeLabel(item.type, types.data)}</td>
              <td className="numeric">{item.capacityKg == null ? '' : formatKg(item.capacityKg)}</td>
              <td><Status status={status[item.id]} /></td>
            </tr>
          ))}
        </SortableTable>
      )}
      {types.error ? <LoadError load={types} before="Les types d’équipement ne se chargent pas." /> : types.data && <Types types={types.data} />}
    </main>
  )
}
