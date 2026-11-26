import { useState } from 'react'

// Values and errors of a form, kept here so each page only lists its fields
export function useForm(initial) {
  const [values, change] = useState(initial)
  const [start, setStart] = useState(initial)
  const [errors, setErrors] = useState({})
  function set(changes) {
    change((before) => ({ ...before, ...changes }))
    setErrors((before) => Object.fromEntries(Object.entries(before).filter(([key]) => !(key in changes))))
  }
  const text = (name, more) => (props) => <input type="text" autoComplete="off" {...more} {...props} value={values[name] ?? ''} onChange={(event) => set({ [name]: event.target.value })} />
  // Starts again from these values: nothing is unsaved
  function setValues(next) {
    change(next)
    setStart(next)
  }
  const dirty = Object.keys(values).some((key) => (values[key] ?? '') !== (start[key] ?? ''))
  // Shows what is missing and puts the keyboard on the first one
  function accept(found, form) {
    setErrors(found)
    if (Object.keys(found).length === 0) return true
    requestAnimationFrame(() => form.querySelector('[aria-invalid=true]')?.focus())
    return false
  }
  return { values, errors, set, setValues, text, accept, dirty }
}
