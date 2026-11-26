import { useState } from 'react'
import { getAvailableTeamLeaders } from '../../api/planning.js'
import { actionsFor, tourWindow } from '../../planning/tour.js'
import { useLoad } from '../../planning/useLoad.js'
import TeamLeaderField from './TeamLeaderField.jsx'

// The saved tour's real window, so nothing is guessed here
const loadLeaders = async (key) => {
  const [start, end] = key.split('|')
  return { leaders: await getAvailableTeamLeaders(start, end) }
}

function LeaderChange({ tour, busy, onChange, onCancel }) {
  const [leader, setLeader] = useState(tour.teamLeaderId ? { id: tour.teamLeaderId, fullName: tour.teamLeaderName ?? 'Non renseigné' } : null)
  const { start, end } = tourWindow(tour)
  const resources = useLoad(`${start}|${end}`, loadLeaders)
  const free = resources.data?.leaders.some(({ id }) => id === leader?.id) ?? false
  const changed = free && leader.id !== tour.teamLeaderId
  return (
    <div className="leaving">
      <TeamLeaderField leader={leader} resources={resources} onChange={setLeader} />
      <button className="button-quiet" type="button" aria-disabled={busy || !changed} onClick={() => changed && onChange(leader.id)}>{busy ? 'Enregistrement…' : 'Changer le chef'}</button>
      <button className="button-quiet" type="button" autoFocus onClick={() => !busy && onCancel()}>Retour</button>
    </div>
  )
}

// replaces: an unsaved tour in the builder would be lost by editing this one
export default function TourActions({ tour, busy, replaces, onConfirm, onEdit, onLeader, onDelete, onCancelTour }) {
  const [asking, setAsking] = useState(null)
  const [reason, setReason] = useState('')
  const allowed = actionsFor(tour.status)
  const [now] = useState(() => Date.now())
  const past = Date.parse(tour.plannedStartTime) < now
  const stop = () => !busy && setAsking(null)

  if (asking === 'leader') return <LeaderChange tour={tour} busy={busy} onChange={onLeader} onCancel={stop} />

  const questions = {
    confirm: { text: 'Le chef d’équipe, les ouvriers, les équipements et les vergers seront réservés.', label: 'Confirmer la tournée', busyLabel: 'Confirmation…', run: onConfirm },
    delete: { text: 'Le brouillon sera supprimé définitivement.', label: 'Supprimer', busyLabel: 'Suppression…', run: onDelete, danger: true },
    edit: { text: 'Une autre tournée en préparation n’est pas enregistrée. Elle sera perdue.', label: 'Modifier ce brouillon', busyLabel: 'Ouverture…', run: onEdit, danger: true },
    cancel: { text: 'La tournée sera annulée définitivement et ses ressources libérées.', label: 'Annuler la tournée', busyLabel: 'Annulation…', run: () => onCancelTour(reason.trim()), danger: true },
  }
  const question = questions[asking]
  if (question) {
    return (
      <div className="leaving" role="group" aria-label={question.label}>
        <p>{question.text}</p>
        {asking === 'confirm' && past && <p className="urgent">Attention : le départ prévu est déjà passé.</p>}
        {asking === 'cancel' && <label className="field">Motif (facultatif)<textarea rows={2} maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} /></label>}
        <button className={question.danger ? 'button-quiet danger' : 'button-quiet'} type="button" aria-disabled={busy} onClick={() => !busy && question.run()}>{busy ? question.busyLabel : question.label}</button>
        <button className="button-quiet" type="button" autoFocus aria-disabled={busy} onClick={stop}>Retour</button>
      </div>
    )
  }

  return (
    <>
      {allowed.includes('confirm') && <button className="button" type="button" onClick={() => setAsking('confirm')}>Confirmer</button>}
      {allowed.includes('edit') && (
        <div className="foot-pair">
          <button className="button-quiet" type="button" aria-disabled={busy} onClick={() => (replaces ? setAsking('edit') : onEdit())}>Modifier</button>
          <button className="button-quiet" type="button" onClick={() => setAsking('leader')}>Changer le chef</button>
          <button className="button-quiet danger" type="button" onClick={() => setAsking('delete')}>Supprimer</button>
        </div>
      )}
      {allowed.includes('cancel') && <button className="button-quiet danger" type="button" onClick={() => setAsking('cancel')}>Annuler la tournée</button>}
    </>
  )
}
