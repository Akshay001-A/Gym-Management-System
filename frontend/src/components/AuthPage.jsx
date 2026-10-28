import { useState } from 'react'
import { BadgeCheck, Dumbbell, LockKeyhole, UserRound } from 'lucide-react'
import { login, registerMember } from '../api/auth'

const initialForm = { name: '', email: '', password: '', age: '', mobile: '', gymTime: 'Morning', gender: '', fatherName: '' }

export default function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [values, setValues] = useState(initialForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const isRegister = mode === 'register'

  function change(event) { setValues((current) => ({ ...current, [event.target.name]: event.target.value })) }

  function navigateTo(event, sectionId) {
    event.preventDefault()
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const session = isRegister
        ? await registerMember(
          { name: values.name.trim(), email: values.email.trim(), password: values.password },
          { name: values.name.trim(), email: values.email.trim(), age: Number(values.age), mobile: values.mobile.trim(), gymTime: values.gymTime, gender: values.gender, fatherName: values.fatherName.trim(), amount: 0 },
        )
        : await login(values.email.trim(), values.password)
      onAuthenticated(session)
    } catch (requestError) { setError(requestError.message) }
    finally { setBusy(false) }
  }

  return (
    <main className="auth-screen">
      <header className="auth-topbar">
        <a className="auth-logo" href="#welcome" aria-label="Form and Fit home"><img src="/images/form-fit-gym-logo.svg" alt="FORM & FIT Fitness Club" /></a>
        <nav aria-label="Main navigation"><a href="#welcome" onClick={(event) => navigateTo(event, 'welcome')}>Our club</a><a href="#club-benefits" onClick={(event) => navigateTo(event, 'club-benefits')}>Member benefits</a></nav>
      </header>
      <div className="auth-layout" id="welcome">
        <section className="auth-intro" id="club-benefits">
          <p className="auth-eyebrow"><span /> {isRegister ? 'YOUR NEXT CHAPTER STARTS HERE' : 'WELCOME BACK TO FORM & FIT'}</p>
          <h1>Build strength.<br /><em>Find your rhythm.</em></h1>
          <p className="auth-intro-copy">A welcoming place to train, feel stronger, and make fitness part of your everyday routine.</p>
          <div className="auth-benefits">
            <article><span><Dumbbell size={19} /></span><div><strong>Train your way</strong><small>Choose a time that fits your day.</small></div></article>
            <article><span><BadgeCheck size={19} /></span><div><strong>Your club, in one place</strong><small>Keep track of memberships and payments.</small></div></article>
          </div>
          <p className="auth-trust"><span><BadgeCheck size={16} /></span> A supportive space for every step of your fitness journey</p>
        </section>
        <section className="auth-card" id="member-access">
          <p className="auth-eyebrow">{isRegister ? 'YOUR FITNESS JOURNEY' : 'MEMBER ACCESS'}</p>
          <h2>{isRegister ? 'Create your account' : 'Welcome back'}</h2>
          <p className="auth-description">{isRegister ? 'Create your member login and profile.' : 'Sign in with the account provided by your club.'}</p>
          <div className="auth-tabs" role="tablist" aria-label="Account access">
            <button type="button" role="tab" aria-selected={!isRegister} className={!isRegister ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('login'); setError('') }}>Sign in</button>
            <button type="button" role="tab" aria-selected={isRegister} className={isRegister ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('register'); setError('') }}>Join as member</button>
          </div>
          <form className={isRegister ? 'auth-form is-register' : 'auth-form'} onSubmit={submit}>
            {isRegister && <label>Full name<input name="name" autoComplete="name" maxLength="100" value={values.name} onChange={change} required /></label>}
            {isRegister ? <div className="auth-register-credentials">
              <label>Email address<input name="email" type="email" autoComplete="email" value={values.email} onChange={change} required /></label>
              <label>Password<input name="password" type="password" autoComplete="new-password" minLength="8" value={values.password} onChange={change} required /></label>
            </div> : <>
              <label>Email address<input name="email" type="email" autoComplete="email" value={values.email} onChange={change} required /></label>
              <label>Password<input name="password" type="password" autoComplete="current-password" minLength="8" value={values.password} onChange={change} required /></label>
            </>}
            {isRegister && <>
              <div className="auth-form-row">
                <label>Age<input name="age" type="number" min="1" max="120" value={values.age} onChange={change} required /></label>
                <label>Mobile<input name="mobile" inputMode="tel" maxLength="20" value={values.mobile} onChange={change} required /></label>
              </div>
              <div className="auth-form-row">
                <label>Preferred gym time<select name="gymTime" value={values.gymTime} onChange={change}><option>Morning</option><option>Afternoon</option><option>Evening</option></select></label>
                <label>Gender<input name="gender" maxLength="20" value={values.gender} onChange={change} required /></label>
              </div>
              <label>Parent / guardian name<input name="fatherName" maxLength="100" value={values.fatherName} onChange={change} required /></label>
            </>}
            {error && <p className="auth-error" role="alert">{error}</p>}
            <button className="auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : <><LockKeyhole size={17} />{isRegister ? 'Create member account' : 'Sign in'}</>}</button>
          </form>
          <div className="auth-footnote"><UserRound size={15} /><span>Your role is assigned by the backend after authentication.</span></div>
          <p className="auth-admin-note">Administrator access is created through protected first-admin setup, not member registration.</p>
        </section>
      </div>
      <footer className="auth-footer"><span>FORM &amp; FIT FITNESS CLUB</span><span>Move well. Feel good. Keep going.</span></footer>
    </main>
  )
}
