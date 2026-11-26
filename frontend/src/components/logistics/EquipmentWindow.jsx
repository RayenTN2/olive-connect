import { useId } from 'react'
import { createResource, getResources, updateResource } from '../../api/logistics.js'
import { byType, typeLabel } from '../../logistics/calendar.js'
import { checkEquipment, equipmentBody, hasField } from '../../logistics/forms.js'
import { loadTypes } from '../../logistics/loaders.js'
import { sameFields, saveChange, saveNew } from '../../logistics/save.js'
import { useRun } from '../../logistics/useRun.js'
import { useLoad } from '../../planning/useLoad.js'
import FormWindow from './FormWindow.jsx'
import { Field, Icon, LoadError, Skeleton } from './parts.jsx'
import { useForm } from './useForm.jsx'

const list = () => getResources('equipment')
const ICONS = {
  BIN: 'M2.500 4h15v3.500h-15ZM4 7.500l1.200 9h9.600l1.200-9M8 10.500v3m4-3v3',
  TRACTOR: 'M2.500 12.500h3m5 0h3V8h-3L9 3.500H4.500v9M13.500 8h2.500v2M4.500 16.800a2.400 2.400 0 1 0 0-4.800 2.400 2.400 0 0 0 0 4.800Zm10.500 0a3.400 3.400 0 1 0 0-6.800 3.400 3.400 0 0 0 0 6.800Z',
  NET: 'M3 3h14v14H3Zm0 4.700h14M3 12.300h14M7.700 3v14m4.600-14v14',
  VIBRATOR: 'M10 3v14M6.500 6v8m7-8v8M3 8.500v3m14-3v3',
}
const OTHER = 'M10 2.500 16.500 6v8L10 17.500 3.500 14V6Zm-6.500 3.500L10 9.700 16.500 6M10 9.700v7.800'
const iconOf = (code) => ICONS[code] ?? OTHER
const fields = ({ name, reference, type, brand, model, serialNumber, registrationNumber, capacityKg }) => ({ name, reference, type, brand, model, serialNumber, registrationNumber, capacityKg: capacityKg ?? '' })

function TypeCards({ types, value, error, onChange }) {
  const id = useId()
  return (
    <fieldset className="type-cards">
      <legend>Type</legend>
      <div>
        {types.map(({ code }) => code).sort(byType).map((code) => (
          <label key={code}>
            <input type="radio" name={id} value={code} checked={value === code} aria-invalid={error ? true : undefined} aria-describedby={error ? `${id}-note` : undefined} onChange={() => onChange(code)} />
            <Icon path={iconOf(code)} />
            {typeLabel(code, types)}
          </label>
        ))}
      </div>
      {error && <span id={`${id}-note`} className="field-hint urgent">{error}</span>}
    </fieldset>
  )
}

// item: null for a new one; read: the item as it is now, for an existing one
export default function EquipmentWindow({ item, read, onSaved, onChangedElsewhere, onClose }) {
  const types = useLoad('types', loadTypes)
  const { values, errors, set, text, accept, setValues, dirty } = useForm(item ? fields(item) : {})
  const run = useRun()
  const type = types.data?.find(({ code }) => code === values.type)

  function submit(event) {
    event.preventDefault()
    if (!types.data || !accept(checkEquipment(values, type), event.currentTarget)) return
    const body = equipmentBody(values, type)
    const matches = sameFields(body)
    run.run(async () => {
      try {
        onSaved(item
          ? await saveChange({ base: item, read, send: () => updateResource('equipment', item.id, body), matches })
          : await saveNew({ list, send: () => createResource('equipment', body), matches }))
      } catch (failure) {
        if (failure.current) {
          setValues(fields(failure.current))
          onChangedElsewhere(failure.current)
        }
        throw failure
      }
    })
  }

  return (
    <FormWindow
      label={item ? 'Modifier l’équipement' : 'Nouvel équipement'} name={(values.name ?? '').trim()} hint={type ? typeLabel(type.code, types.data) : 'Choisissez un type'}
      icon={<Icon path={iconOf(values.type)} />} submitLabel={item ? 'Enregistrer' : 'Ajouter l’équipement'} ready={types.data !== null}
      busy={run.busy} error={run.error} dirty={dirty} onSubmit={submit} onClose={onClose}
    >
      {types.error && <LoadError load={types} />}
      {!types.error && !types.data && <Skeleton label="Chargement des types" />}
      {types.data && (
        <>
          <TypeCards types={types.data} value={values.type} error={errors.type} onChange={(code) => set({ type: code })} />
          <div className="field-pair">
            <Field label="Nom" error={errors.name}>{text('name', { placeholder: 'Benne 3' })}</Field>
            <Field label="Référence, unique" error={errors.reference}>{text('reference', { placeholder: 'BN-003' })}</Field>
          </div>
          {hasField(type, 'capacityKg') && <Field label="Capacité (kg)" error={errors.capacityKg}>{text('capacityKg', { inputMode: 'numeric', placeholder: '500' })}</Field>}
          {hasField(type, 'registrationNumber') && <Field label="Immatriculation, facultatif" error={errors.registrationNumber}>{text('registrationNumber', { placeholder: '120 TU 4567' })}</Field>}
          <details className="window-more" open={Boolean(item && (item.brand || item.model || item.serialNumber))}>
            <summary>Marque, modèle, numéro de série</summary>
            <div>
              <div className="field-pair">
                <Field label="Marque, facultatif">{text('brand')}</Field>
                <Field label="Modèle, facultatif">{text('model')}</Field>
              </div>
              <Field label="Numéro de série, facultatif">{text('serialNumber')}</Field>
            </div>
          </details>
        </>
      )}
    </FormWindow>
  )
}
