import { useEffect, useState } from 'react'
import { Dumbbell } from 'lucide-react'
import { clearSession, getSession } from '../api/client'
import AuthPage from './AuthPage'
import MemberPortal from './MemberPortal'

export default function AuthGate({ children }) {
  const [session, setSession] = useState(getSession)

  useEffect(() => {
    const expired = () => setSession(null)
    window.addEventListener('gym-auth-expired', expired)
    return () => window.removeEventListener('gym-auth-expired', expired)
  }, [])

  function logout() {
    clearSession()
    setSession(null)
  }

  if (!session) return <AuthPage onAuthenticated={setSession} />
  if (session.role === 'ADMIN') return children({ session, onLogout: logout })
  if (session.role === 'MEMBER') return <MemberPortal session={session} onLogout={logout} onSessionUpdated={setSession} />
  return <main className="auth-screen"><section className="auth-card"><div className="auth-brand"><span><Dumbbell size={22} /></span><div><strong>FORM &amp; FIT</strong><small>CLUB MANAGEMENT</small></div></div><h1>Role not available here</h1><p className="auth-description">This frontend currently supports administrator and member workspaces. Your role is {session.role}.</p><button className="auth-submit" type="button" onClick={logout}>Sign out</button></section></main>
}
