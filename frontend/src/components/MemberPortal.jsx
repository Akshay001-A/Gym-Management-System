import { useCallback, useEffect, useMemo, useState } from 'react'
import { Activity, BadgeCheck, CreditCard, Dumbbell, LogOut, RefreshCw, UserRound } from 'lucide-react'
import {
  cancelMyMembership, enrollInPlan, getMemberTrainers, getMembershipPlans,
  getMyMemberships, getMyPayments, getMyProfile, renewPlan, simulatePaymentOutcome,
  startSimulatedPayment, updateMyProfile,
} from '../api/member'

const emptyProfile = { name: '', age: '', mobile: '', gymTime: '', email: '', gender: '', fatherName: '' }
const pages = { home: 'Overview', memberships: 'My Memberships', trainers: 'Trainers', payments: 'Payment History', profile: 'My Profile' }

export default function MemberPortal({ session, onLogout }) {
  const [page, setPage] = useState('home')
  const [profile, setProfile] = useState(null)
  const [memberships, setMemberships] = useState([])
  const [plans, setPlans] = useState([])
  const [trainers, setTrainers] = useState([])
  const [payments, setPayments] = useState([])
  const [today, setToday] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const [notice, setNotice] = useState('')
  const [editing, setEditing] = useState(false)
  const [profileForm, setProfileForm] = useState(emptyProfile)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    const results = await Promise.allSettled([
      getMyProfile(), getMyMemberships(), getMembershipPlans(), getMemberTrainers(), getMyPayments(),
    ])
    setToday(new Date().toISOString().slice(0, 10))
    if (results[0].status === 'fulfilled') {
      setProfile(results[0].value)
      setProfileForm(Object.fromEntries(Object.keys(emptyProfile).map((key) => [key, results[0].value[key] ?? ''])))
    } else setError(results[0].reason.message)
    if (results[1].status === 'fulfilled') setMemberships(results[1].value)
    if (results[2].status === 'fulfilled') setPlans(results[2].value)
    if (results[3].status === 'fulfilled') setTrainers(results[3].value)
    if (results[4].status === 'fulfilled') setPayments(results[4].value?.content ?? [])
    setLoading(false)
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { refresh() }, 0)
    return () => window.clearTimeout(timer)
  }, [refresh])

  const currentMembership = useMemo(() => memberships.find((item) =>
    item.status === 'ACTIVE' && item.endDate >= today), [memberships, today])

  async function changePlan(plan, isRenewal) {
    setBusy('plan')
    setNotice('')
    try {
      if (isRenewal) await renewPlan(plan)
      else await enrollInPlan(plan)
      setNotice(isRenewal ? 'Renewal scheduled.' : 'Membership started.')
      await refresh()
    } catch (requestError) { setNotice(requestError.message) }
    finally { setBusy('') }
  }

  async function cancelMembership(id) {
    setBusy('membership-' + id)
    try {
      await cancelMyMembership(id)
      setNotice('Membership cancelled.')
      await refresh()
    } catch (requestError) { setNotice(requestError.message) }
    finally { setBusy('') }
  }

  async function payDemo(id) {
    setBusy('payment-' + id)
    setNotice('')
    try {
      const key = window.crypto?.randomUUID?.() ?? ('web-' + Date.now() + '-' + Math.random().toString(16).slice(2))
      const pending = await startSimulatedPayment(id, key)
      await simulatePaymentOutcome(pending.id, 'SUCCESS')
      setNotice('Demo payment recorded as successful. No money was charged.')
      await refresh()
    } catch (requestError) { setNotice(requestError.message) }
    finally { setBusy('') }
  }

  async function saveProfile(event) {
    event.preventDefault()
    setBusy('profile')
    try {
      const updated = await updateMyProfile({
        name: profileForm.name, age: Number(profileForm.age), mobile: profileForm.mobile,
        gymTime: profileForm.gymTime, email: profileForm.email,
        gender: profileForm.gender, fatherName: profileForm.fatherName,
      })
      setProfile(updated)
      setEditing(false)
      setNotice('Profile updated.')
    } catch (requestError) { setNotice(requestError.message) }
    finally { setBusy('') }
  }

  function changeProfile(event) {
    setProfileForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  return (
    <div className="member-app">
      <header className="member-header">
        <a className="member-brand" href="#member-home" onClick={(event) => { event.preventDefault(); setPage('home') }}><span><Dumbbell size={21} /></span><strong>FORM &amp; FIT</strong></a>
        <div className="member-header-user"><span>{profile?.name || session.name}</span><button type="button" onClick={onLogout}><LogOut size={16} /> Sign out</button></div>
      </header>
      <main className="member-content">
        <section className="member-welcome">
          <div><p className="member-eyebrow">MEMBER WORKSPACE</p><h1>Welcome, {profile?.name || session.name}</h1><p>Your membership and club information in one place.</p></div>
          <span className="member-welcome-icon"><Activity size={28} /></span>
        </section>
        <nav className="member-tabs" aria-label="Member pages">
          {Object.entries(pages).map(([key, label]) => <button type="button" key={key} className={page === key ? 'selected' : ''} onClick={() => setPage(key)}>{label}</button>)}
          <button type="button" className="member-refresh" onClick={refresh} aria-label="Refresh member information"><RefreshCw size={16} /></button>
        </nav>
        {notice && <div className="member-notice" role="status">{notice}</div>}
        {error && <div className="member-error" role="alert">{error}<button type="button" onClick={refresh}>Retry</button></div>}
        {loading ? <div className="member-loading">Loading your club information…</div> : <>
          {page === 'home' && <>
            <section className="member-stat-grid">
              <article><span><BadgeCheck size={18} /></span><small>MEMBERSHIP STATUS</small><strong>{currentMembership?.status ?? 'No active plan'}</strong></article>
              <article><span><CreditCard size={18} /></span><small>PLAN</small><strong>{currentMembership?.planName ?? 'Choose a plan'}</strong></article>
              <article><span><UserRound size={18} /></span><small>MEMBER ID</small><strong>#{session.memberId ?? profile?.id ?? '—'}</strong></article>
            </section>
            <section className="member-panel">
              <div className="member-section-heading"><div><h2>Membership plans</h2><p>Prices and durations come from the Membership Service.</p></div></div>
              <PlanCards plans={plans} active={Boolean(currentMembership)} busy={busy} onChoose={(plan) => changePlan(plan, Boolean(currentMembership))} />
            </section>
            <section className="member-panel">
              <div className="member-section-heading"><div><h2>My memberships</h2><p>Personal membership records and status.</p></div><button className="member-text-button" type="button" onClick={() => setPage('memberships')}>View all</button></div>
              {memberships.length ? <MembershipList items={memberships.slice(0, 3)} busy={busy} onCancel={cancelMembership} onPay={payDemo} /> : <p className="member-empty">No membership records yet. Choose a plan above to get started.</p>}
            </section>
            <section className="member-panel">
              <div className="member-section-heading"><div><h2>Meet the trainers</h2><p>Browse the coaching team at your club.</p></div><button className="member-text-button" type="button" onClick={() => setPage('trainers')}>View all</button></div>
              <TrainerList trainers={trainers.slice(0, 4)} />
            </section>
            <PaymentHistory payments={payments} />
          </>}
          {page === 'memberships' && <section className="member-panel"><div className="member-section-heading"><div><h2>My memberships</h2><p>Memberships are tied to your member profile.</p></div></div>{memberships.length ? <MembershipList items={memberships} busy={busy} onCancel={cancelMembership} onPay={payDemo} /> : <p className="member-empty">No membership records yet.</p>}</section>}
          {page === 'trainers' && <section className="member-panel"><div className="member-section-heading"><div><h2>Trainers</h2><p>Trainer directory from the Trainer Service.</p></div></div><TrainerList trainers={trainers} /></section>}
          {page === 'payments' && <PaymentHistory payments={payments} />}
          {page === 'profile' && <section className="member-panel">
            <div className="member-section-heading"><div><h2>My profile</h2><p>Update your own contact information.</p></div>{!editing && <button className="member-text-button" type="button" onClick={() => setEditing(true)}>Edit profile</button>}</div>
            {editing ? <form className="member-profile-form" onSubmit={saveProfile}>
              {[['name','Full name'],['email','Email address'],['age','Age'],['mobile','Mobile'],['gymTime','Preferred gym time'],['gender','Gender'],['fatherName','Parent / guardian']].map(([key,label]) => <label key={key}>{label}<input name={key} type={key === 'email' ? 'email' : key === 'age' ? 'number' : 'text'} min={key === 'age' ? '1' : undefined} value={profileForm[key]} onChange={changeProfile} required /></label>)}
              <div className="member-profile-actions"><button type="button" onClick={() => setEditing(false)}>Cancel</button><button type="submit" disabled={busy === 'profile'}>{busy === 'profile' ? 'Saving…' : 'Save profile'}</button></div>
            </form> : <dl className="member-profile-list">{Object.entries({Name:profile?.name, Email:profile?.email, Age:profile?.age, Mobile:profile?.mobile, 'Gym time':profile?.gymTime, Gender:profile?.gender, 'Member ID':profile?.id}).map(([key,value]) => <div key={key}><dt>{key}</dt><dd>{value ?? '—'}</dd></div>)}</dl>}
          </section>}
        </>}
        <footer className="member-footer">Your account role and member identity are verified by the backend.</footer>
      </main>
    </div>
  )
}

function PaymentHistory({ payments }) {
  return <section className="member-panel">
    <div className="member-section-heading"><div><h2>Payment history</h2><p>Demo payments only. No real payment is collected by this app.</p></div></div>
    {payments.length ? <div className="member-table-scroll"><table><thead><tr><th>Payment</th><th>Membership</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>#{payment.id}</td><td>#{payment.membershipId}</td><td>₹{payment.amount}</td><td>{payment.status}</td><td>{payment.createdAt ? new Date(payment.createdAt).toLocaleDateString() : '—'}</td></tr>)}</tbody></table></div> : <p className="member-empty">No payment records yet.</p>}
  </section>
}

function PlanCards({ plans, active, busy, onChoose }) {
  if (!plans.length) return <p className="member-empty">Membership plans are not available right now.</p>
  return <div className="member-plan-grid">{plans.map((plan) => <article className="member-plan-card" key={plan.name}>
    <div><span>{plan.name}</span><strong>₹{Number(plan.price).toLocaleString('en-IN')}</strong><small>{plan.durationMonths} month{plan.durationMonths === 1 ? '' : 's'}</small></div>
    <button type="button" disabled={busy === 'plan'} onClick={() => onChoose(plan.name)}>{active ? 'Schedule renewal' : 'Choose plan'}</button>
  </article>)}</div>
}

function MembershipList({ items, busy, onCancel, onPay }) {
  return <div className="member-table-scroll"><table><thead><tr><th>Plan</th><th>Price</th><th>Dates</th><th>Status</th><th>Actions</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}>
    <td>{item.planName}</td><td>₹{item.price}</td><td>{item.startDate} – {item.endDate}</td><td><span className="member-status">{item.status}</span></td>
    <td><div className="member-row-actions">{item.status === 'ACTIVE' && <><button type="button" disabled={Boolean(busy)} onClick={() => onPay(item.id)}>Demo pay</button><button type="button" disabled={Boolean(busy)} onClick={() => onCancel(item.id)}>Cancel</button></>}</div></td>
  </tr>)}</tbody></table></div>
}

function TrainerList({ trainers }) {
  if (!trainers.length) return <p className="member-empty">No trainer records are available right now.</p>
  return <div className="member-trainer-grid">{trainers.map((trainer) => <article key={trainer.id}><span><Dumbbell size={17} /></span><div><strong>{trainer.name}</strong><small>{trainer.specialization || 'Trainer'} · {trainer.experienceYears ?? 0} years</small></div><em>{trainer.availability || trainer.status || ''}</em></article>)}</div>
}
