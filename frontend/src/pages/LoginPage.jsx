import { useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { loginErrorMessage, SESSION_EXPIRED } from '../auth/messages.js'
import Brand from '../components/Brand.jsx'
import './LoginPage.css'

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/

export default function LoginPage() {
  const { login, notice } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [busy, setBusy] = useState(false)
  // field: 'email' | 'password' | 'both' (credentials refused) | null (not the fields' fault)
  const [error, setError] = useState(null)
  const emailRef = useRef(null)
  const passwordRef = useRef(null)

  const message = error?.text ?? (notice === 'expired' ? SESSION_EXPIRED : '')

  function edit(setValue) {
    return (event) => {
      setValue(event.target.value)
      setError(null)
    }
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const address = email.trim()
    if (!EMAIL_PATTERN.test(address)) {
      setError({
        field: 'email',
        text: address ? 'Cette adresse e-mail est incomplète. Exemple : nom@exemple.tn' : 'Saisissez votre adresse e-mail.',
      })
      emailRef.current.focus()
      return
    }
    if (!password) {
      setError({ field: 'password', text: 'Saisissez votre mot de passe.' })
      passwordRef.current.focus()
      return
    }

    setError(null)
    setBusy(true)
    try {
      await login(address, password) // App then redirects
    } catch (failure) {
      const refused = failure.status === 401 && failure.message.includes('Invalid email or password')
      setBusy(false)
      setError({ field: refused ? 'both' : null, text: loginErrorMessage(failure.status, failure.message) })
      if (refused) {
        passwordRef.current.focus()
        passwordRef.current.select()
      }
    }
  }

  const invalid = (field) => error?.field === field || error?.field === 'both'
  const cell = (field) => (error?.field === field ? 'cell bad' : 'cell')

  return (
    <div className="login">
      <section className="login-intro">
        <Brand />
        <h1><span>Connectez-vous</span> à votre espace</h1>
        <p className="login-lead">Accès réservé aux comptes validés par un administrateur de la coopérative.</p>
        <p className="login-foot">Gestion des collectes oléicoles</p>
      </section>
      <main className="login-work">
        <form noValidate aria-label="Connexion" onSubmit={submit}>
          <div className={error?.field === 'both' ? 'fields refused' : 'fields'}>
            <div className={cell('email')}>
              <label htmlFor="email">Adresse e-mail</label>
              <input
                id="email" ref={emailRef} type="email" autoComplete="username" spellCheck={false} autoFocus
                value={email} onChange={edit(setEmail)} readOnly={busy}
                aria-invalid={invalid('email')} aria-describedby="login-message"
              />
            </div>
            <div className={cell('password')}>
              <label htmlFor="password">Mot de passe</label>
              <input
                id="password" ref={passwordRef} type={visible ? 'text' : 'password'} autoComplete="current-password"
                value={password} onChange={edit(setPassword)} readOnly={busy}
                aria-invalid={invalid('password')} aria-describedby="login-message"
              />
              <button
                className="reveal" type="button" onClick={() => setVisible(!visible)}
                aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
              >
                {visible ? 'Masquer' : 'Afficher'}
              </button>
            </div>
          </div>
          <div id="login-message" role="alert">
            {message && (
              <p className={error ? 'login-message' : 'login-message info'}>
                <svg viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true">
                  <circle cx="9" cy="9" r="7" />
                  <path d="M9 5.4v4.2M9 12.4v.1" />
                </svg>
                {message}
              </p>
            )}
          </div>
          <button className="button" aria-disabled={busy}>
            {busy && <i className="spinner" aria-hidden="true" />}
            {busy ? 'Connexion en cours…' : 'Se connecter'}
          </button>
        </form>
      </main>
    </div>
  )
}
