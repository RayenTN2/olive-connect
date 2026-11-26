import { createResource, getResources, updateResource } from '../../api/logistics.js'
import { checkWorker, workerBody } from '../../logistics/forms.js'
import { sameFields, saveChange, saveNew } from '../../logistics/save.js'
import { useRun } from '../../logistics/useRun.js'
import FormWindow from './FormWindow.jsx'
import { initials } from './names.js'
import { Field, Icon } from './parts.jsx'
import { useForm } from './useForm.jsx'

const list = () => getResources('workers')
const PERSON = 'M10 9.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm-6 7.5c0-3.300 2.400-5.500 6-5.500s6 2.200 6 5.500'

// worker: null for a new one; read: the worker as it is now, for an existing one
export default function WorkerWindow({ worker, read, onSaved, onChangedElsewhere, onClose }) {
  const { values, errors, text, accept, setValues, dirty } = useForm(worker ? workerBody(worker) : {})
  const run = useRun()
  const name = (values.fullName ?? '').trim()

  function submit(event) {
    event.preventDefault()
    if (!accept(checkWorker(values), event.currentTarget)) return
    const body = workerBody(values)
    const matches = sameFields(body)
    run.run(async () => {
      try {
        onSaved(worker
          ? await saveChange({ base: worker, read, send: () => updateResource('workers', worker.id, body), matches })
          : await saveNew({ list, send: () => createResource('workers', body), matches }))
      } catch (failure) {
        if (failure.current) {
          setValues(workerBody(failure.current))
          onChangedElsewhere(failure.current)
        }
        throw failure
      }
    })
  }

  return (
    <FormWindow
      label={worker ? 'Modifier l’ouvrier' : 'Nouvel ouvrier'} name={name} hint={worker ? 'Modifier les informations' : 'Disponible dès l’enregistrement'}
      icon={initials(name) || <Icon path={PERSON} />} submitLabel={worker ? 'Enregistrer' : 'Ajouter l’ouvrier'}
      busy={run.busy} error={run.error} dirty={dirty} onSubmit={submit} onClose={onClose}
    >
      <Field label="Nom complet" error={errors.fullName}>{text('fullName', { placeholder: 'Ali Ben Salah' })}</Field>
      <Field label="Téléphone" error={errors.phone}>{text('phone', { type: 'tel', placeholder: '20 000 000' })}</Field>
    </FormWindow>
  )
}
