import { useEffect, useId, useRef } from 'react'
import { STATUSES } from '../../logistics/calendar.js'

export function Status({ status }) {
  return <span className={`tour-status ${status}`}>{STATUSES[status]}</span>
}

export function Icon({ path }) {
  return <svg viewBox="0 0 20 20" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true"><path d={path} /></svg>
}

export function LoadError({ load, before = '' }) {
  return <div className="notice" role="alert"><p>{before} {load.error}</p><button className="button-quiet" type="button" onClick={load.retry}>Réessayer</button></div>
}

export function Skeleton({ label }) {
  return <div className="loading" role="status"><span className="visually-hidden">{label}</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>
}

// children: (props) => the control
export function Field({ label, hint, error, children }) {
  const id = useId()
  const note = error ?? hint
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children({ id, 'aria-invalid': error ? true : undefined, 'aria-describedby': note ? `${id}-note` : undefined })}
      {note && <span id={`${id}-note`} className={error ? 'field-hint urgent' : 'field-hint muted'}>{note}</span>}
    </div>
  )
}

export function FormButtons({ busy, label, onCancel }) {
  return (
    <div className="form-buttons">
      <button className="button" type="submit" aria-disabled={busy}>{busy ? 'Enregistrement…' : label}</button>
      {onCancel && <button className="button-quiet" type="button" aria-disabled={busy} onClick={() => !busy && onCancel()}>Annuler</button>}
    </div>
  )
}

// A question before something that matters. children: what will happen
export function Ask({ label, danger, busy, onConfirm, onCancel, children }) {
  return (
    <div className="leaving" role="group" aria-label={label}>
      {children}
      <button className={danger ? 'button-quiet danger' : 'button-quiet'} type="button" aria-disabled={busy} onClick={() => !busy && onConfirm()}>{busy ? 'Enregistrement…' : label}</button>
      <button className="button-quiet" type="button" autoFocus aria-disabled={busy} onClick={() => !busy && onCancel()}>Retour</button>
    </div>
  )
}

// The title takes the keyboard when its page opens or its state changes
export function Title({ state, children }) {
  const ref = useRef(null)
  useEffect(() => ref.current.focus(), [state])
  return <h1 ref={ref} tabIndex={-1}>{children}</h1>
}
