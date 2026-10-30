import { useState } from 'react'
import { BadgeCheck, Dumbbell, LockKeyhole, UserRound } from 'lucide-react'
import { completeMemberRegistration, completePasswordReset, login, requestMemberRegistrationCode, requestPasswordResetCode } from '../api/auth'

const initialForm = { email: '', identifier: '', verificationCode: '', password: '', confirmPassword: '' }

export default function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [values, setValues] = useState(initialForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [registrationStep, setRegistrationStep] = useState('contact')
  const [resetStep, setResetStep] = useState('email')
  const [verifiedEmailHint, setVerifiedEmailHint] = useState('')
  const isRegister = mode === 'register'
  const isReset = mode === 'reset'

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
      if (isReset && resetStep === 'email') {
        const result = await requestPasswordResetCode(values.email.trim())
        setVerifiedEmailHint(result.maskedEmail || values.email.trim())
        setResetStep('verify')
      } else if (isReset) {
        if (values.password !== values.confirmPassword) {
          setError('The passwords do not match.')
          return
        }
        await completePasswordReset(values.email.trim(), values.verificationCode.trim(), values.password)
        setMode('login')
        setRegistrationStep('contact')
        setResetStep('email')
        setValues((current) => ({ ...current, password: '', confirmPassword: '', verificationCode: '' }))
        setError('Your password has been reset. Sign in with your new password.')
      } else if (isRegister && registrationStep === 'contact') {
        const result = await requestMemberRegistrationCode(values.identifier.trim())
        setVerifiedEmailHint(result.maskedEmail || '')
        setRegistrationStep('verify')
      } else if (isRegister) {
        if (values.password !== values.confirmPassword) {
          setError('The passwords do not match.')
          return
        }
        const session = await completeMemberRegistration(values.identifier.trim(), values.verificationCode.trim(), values.password)
        onAuthenticated(session)
      } else {
        onAuthenticated(await login(values.email.trim(), values.password))
      }
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
          <h2>{isReset ? resetStep === 'email' ? 'Reset your password' : 'Verify your email' : isRegister ? registrationStep === 'contact' ? 'Create your account' : 'Verify your email' : 'Welcome back'}</h2>
          <p className="auth-description">{isReset ? resetStep === 'email' ? 'Enter the email address on your account and we’ll send a one-time reset code.' : `Enter the code sent to ${verifiedEmailHint || values.email} and choose a new password.` : isRegister ? registrationStep === 'contact' ? 'Use the email or mobile number already registered by your gym.' : `Enter the code sent to ${verifiedEmailHint || 'the email on your gym profile'} and set your password.` : 'Sign in with the account provided by your club.'}</p>
          {!isReset && <div className="auth-tabs" role="tablist" aria-label="Account access">
            <button type="button" role="tab" aria-selected={!isRegister} className={!isRegister ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('login'); setRegistrationStep('contact'); setError('') }}>Sign in</button>
            <button type="button" role="tab" aria-selected={isRegister} className={isRegister ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('register'); setRegistrationStep('contact'); setError('') }}>Join as member</button>
          </div>}
          <form className={isRegister || isReset ? 'auth-form is-register' : 'auth-form'} onSubmit={submit}>
            {isReset && resetStep === 'email' ? <label>Email address<input name="email" type="email" autoComplete="email" value={values.email} onChange={change} required /></label> : isReset ? <>
              <label>6-digit email verification code<input name="verificationCode" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength="6" value={values.verificationCode} onChange={change} required /></label>
              <label>New password<input name="password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={values.password} onChange={change} required /></label>
              <label>Confirm new password<input name="confirmPassword" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={values.confirmPassword} onChange={change} required /></label>
            </> : isRegister && registrationStep === 'contact' ? <label>Email or mobile number<input name="identifier" type="text" autoComplete="username" maxLength="150" value={values.identifier} onChange={change} required /></label> : isRegister ? <>
              <label>6-digit email verification code<input name="verificationCode" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength="6" value={values.verificationCode} onChange={change} required /></label>
              <label>Create password<input name="password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={values.password} onChange={change} required /></label>
              <label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={values.confirmPassword} onChange={change} required /></label>
            </> : <>
              <label>Email address<input name="email" type="email" autoComplete="email" value={values.email} onChange={change} required /></label>
              <label>Password<input name="password" type="password" autoComplete="current-password" minLength="8" value={values.password} onChange={change} required /></label>
            </>}
            {error && <p className={error.startsWith('Your password has been reset') ? 'auth-success' : 'auth-error'} role="status">{error}</p>}
            <button className="auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : <><LockKeyhole size={17} />{isReset ? resetStep === 'email' ? 'Send reset code' : 'Reset password' : isRegister ? registrationStep === 'contact' ? 'Send verification code' : 'Create member account' : 'Sign in'}</>}</button>
          </form>
          {!isRegister && !isReset && <button type="button" className="auth-link-button" onClick={() => { setMode('reset'); setResetStep('email'); setError(''); setValues((current) => ({ ...current, verificationCode: '', password: '', confirmPassword: '' })) }}>Forgot password?</button>}
          {isReset && <button type="button" className="auth-link-button" onClick={() => { setMode('login'); setResetStep('email'); setError('') }}>Back to sign in</button>}
          <div className="auth-footnote"><UserRound size={15} /><span>Your role is assigned by the backend after authentication.</span></div>
          <p className="auth-admin-note">Administrator access is created through protected first-admin setup, not member registration.</p>
        </section>
      </div>
      <footer className="auth-footer"><span>FORM &amp; FIT FITNESS CLUB</span><span>Move well. Feel good. Keep going.</span></footer>
    </main>
  )
}
