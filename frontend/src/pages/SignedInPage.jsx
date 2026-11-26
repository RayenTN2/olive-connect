import { useAuth } from '../auth/AuthContext.jsx'
import { roleLabel } from '../auth/roles.js'
import Brand from '../components/Brand.jsx'

// Temporary landing page until the role spaces exist
export default function SignedInPage() {
  const { user, logout } = useAuth()
  return (
    <main className="signed-in">
      <Brand />
      <h1>Connexion réussie</h1>
      <p>{user.fullName}, {roleLabel(user.role)}</p>
      <button className="button" type="button" onClick={logout}>Se déconnecter</button>
    </main>
  )
}
