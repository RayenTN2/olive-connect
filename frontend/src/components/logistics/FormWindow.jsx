import { useId, useLayoutEffect, useRef, useState } from 'react'
import { Ask } from './parts.jsx'
import '../planning/TourBuilder.css' // field, leaving
import './FormWindow.css'

// A centred window holding one form, over the page. The browser keeps the keyboard inside it and gives it back on closing.
// name: what is being typed, shown as the title; dirty: something would be lost by closing
export default function FormWindow({ label, name, hint, icon, submitLabel, ready = true, busy, error, dirty, onSubmit, onClose, children }) {
  const ref = useRef(null)
  const titleId = useId()
  const [asking, setAsking] = useState(false)

  useLayoutEffect(() => {
    const dialog = ref.current
    dialog.showModal()
    const field = dialog.querySelector('.window-body input:checked') ?? dialog.querySelector('.window-body input')
    field?.focus()
    return () => dialog.close()
  }, [])

  function close() {
    if (busy) return
    if (asking) setAsking(false)
    else if (dirty) setAsking(true)
    else onClose()
  }

  return (
    <dialog
      ref={ref} className="window" aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); close() }}
      // A browser may close it by itself after several Escape presses
      onClose={() => !ref.current?.open && onClose()}
      onMouseDown={(event) => event.target === event.currentTarget && close()}
    >
      <form onSubmit={onSubmit} noValidate>
        <div className="window-head">
          <span className="window-icon" aria-hidden="true">{icon}</span>
          <div className="window-title">
            <h2 id={titleId} aria-label={label}>{name || label}</h2>
            <p className="muted">{hint}</p>
          </div>
          <button className="icon-button" type="button" aria-label="Fermer" onClick={close}>
            <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true"><path d="m5 5 10 10M15 5 5 15" /></svg>
          </button>
        </div>
        <div className="window-body">{children}</div>
        <div className="window-foot">
          {error && <p className="urgent" role="alert">{error}</p>}
          {asking ? (
            <Ask label="Fermer sans enregistrer" danger busy={busy} onConfirm={onClose} onCancel={() => setAsking(false)}><p>Ce que vous avez saisi sera perdu.</p></Ask>
          ) : (
            <div className="window-buttons">
              <button className="button-quiet" type="button" aria-disabled={busy} onClick={close}>Annuler</button>
              {ready && <button className="button" type="submit" aria-disabled={busy}>{busy ? 'Enregistrement…' : submitLabel}</button>}
            </div>
          )}
        </div>
      </form>
    </dialog>
  )
}
