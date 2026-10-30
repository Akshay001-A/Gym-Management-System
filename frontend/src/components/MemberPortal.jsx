import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Activity, BadgeCheck, CreditCard, Dumbbell, LogOut, RefreshCw, UserRound } from 'lucide-react'
import {
  cancelMyMembership, getMemberTrainers, getMembershipPlans,
  getMyMemberships, getMyProfile, recoverAssignedMembershipPlan, updateMyProfile,
} from '../api/member'
import PaymentCenter from './PaymentCenter'
import { getPaymentHistory } from '../api/payments'

const emptyProfile = { name: '', age: '', mobile: '', gymTime: '', email: '', gender: '', fatherName: '' }
const pages = { home: 'Overview', memberships: 'My Memberships', trainers: 'Trainers', payments: 'Payments', profile: 'My Profile' }

async function loadMembershipsWithAssignedPlan() {
  let current = await getMyMemberships()
  if (!current.length) {
    await recoverAssignedMembershipPlan()
    current = await getMyMemberships()
  }
  return current
}

export default function MemberPortal({ session, onLogout }) {
  const [page, setPage] = useState('home')
  const [profile, setProfile] = useState(null)
  const [memberships, setMemberships] = useState([])
  const [plans, setPlans] = useState([])
  const [trainers, setTrainers] = useState([])
  const [selectedPaymentPlan, setSelectedPaymentPlan] = useState('')
  const [today, setToday] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const [notice, setNoticeValue] = useState('')
  const noticeTimer = useRef(null)
  const [editing, setEditing] = useState(false)
  const [profileForm, setProfileForm] = useState(emptyProfile)
  const clearSelectedPaymentPlan = useCallback(() => setSelectedPaymentPlan(''), [])

  function setNotice(message) {
    window.clearTimeout(noticeTimer.current)
    setNoticeValue(message)
    if (message) noticeTimer.current = window.setTimeout(() => setNoticeValue(''), 6000)
  }

  useEffect(() => () => window.clearTimeout(noticeTimer.current), [])
  const refreshMemberships = useCallback(async () => {
    setMemberships(await loadMembershipsWithAssignedPlan())
  }, [])

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try { await getPaymentHistory({ page: 0, size: 100 }) } catch { /* Membership access remains available when payment history is offline. */ }
    const results = await Promise.allSettled([
      getMyProfile(), loadMembershipsWithAssignedPlan(), getMembershipPlans(), getMemberTrainers(),
    ])
    setToday(new Date().toISOString().slice(0, 10))
    if (results[0].status === 'fulfilled') {
      setProfile(results[0].value)
      setProfileForm(Object.fromEntries(Object.keys(emptyProfile).map((key) => [key, results[0].value[key] ?? ''])))
    } else setError(results[0].reason.message)
    if (results[1].status === 'fulfilled') setMemberships(results[1].value)
    if (results[2].status === 'fulfilled') setPlans(results[2].value)
    if (results[3].status === 'fulfilled') setTrainers(results[3].value)
    setLoading(false)
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { refresh() }, 0)
    return () => window.clearTimeout(timer)
  }, [refresh])

  const currentMembership = useMemo(() => memberships.find((item) =>
    item.status === 'ACTIVE' && item.endDate >= today), [memberships, today])

  function choosePlan(plan) {
    setSelectedPaymentPlan(plan)
    setPage('payments')
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

  function payMembership(planName) {
    choosePlan(planName)
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
            <section className="member-panel member-overview-summary">
              <div className="member-section-heading"><div><h2>Your membership</h2><p>A quick look at your current club plan.</p></div><button className="member-text-button" type="button" onClick={() => setPage('memberships')}>View memberships</button></div>
              {currentMembership ? <div className="member-current-plan"><span className="member-current-plan-icon"><BadgeCheck size={19} /></span><div><strong>{currentMembership.planName}</strong><small>Active until {currentMembership.endDate}</small></div><span className="member-status">ACTIVE</span></div> : <p className="member-empty">No active plan. Open membership plans below to get started.</p>}
            </section>
            <details className="member-panel member-collapsible">
              <summary><span><strong>Membership plans</strong><small>Compare prices and durations</small></span><span className="member-detail-count">{plans.length} plans</span></summary>
              <PlanCards plans={plans} busy={busy} onChoose={choosePlan} />
            </details>
            <details className="member-panel member-collapsible">
              <summary><span><strong>Membership history</strong><small>Your plans, payments, and status</small></span><span className="member-detail-count">{memberships.length} records</span></summary>
              {memberships.length ? <MembershipList items={memberships.slice(0, 3)} busy={busy} onCancel={cancelMembership} onPay={payMembership} /> : <p className="member-empty">No membership records yet.</p>}
            </details>
            <details className="member-panel member-collapsible">
              <summary><span><strong>Meet the trainers</strong><small>Browse your club’s coaching team</small></span><span className="member-detail-count">{trainers.length} trainers</span></summary>
              <TrainerList trainers={trainers.slice(0, 4)} />
            </details>
            <details className="member-panel member-collapsible">
              <summary><span><strong>Payment history</strong><small>Verified transactions from the Payment Service</small></span><span className="member-detail-count">View history</span></summary>
              <button className="member-text-button member-payment-link" type="button" onClick={() => setPage('payments')}>Open payment history</button>
            </details>
          </>}
          {page === 'memberships' && <section className="member-panel"><div className="member-section-heading"><div><h2>My memberships</h2><p>Memberships are tied to your member profile.</p></div></div>{memberships.length ? <MembershipList items={memberships} busy={busy} onCancel={cancelMembership} onPay={payMembership} /> : <p className="member-empty">No membership records yet.</p>}</section>}
          {page === 'trainers' && <section className="member-panel"><div className="member-section-heading"><div><h2>Trainers</h2><p>Trainer directory from the Trainer Service.</p></div></div><TrainerList trainers={trainers} /></section>}
          {page === 'payments' && <PaymentCenter initialPlan={selectedPaymentPlan} session={session} onPlanConsumed={clearSelectedPaymentPlan} onMembershipUpdated={refreshMemberships} hasCurrentMembership={Boolean(currentMembership)} />}
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

function PlanCards({ plans, busy, onChoose }) {
  if (!plans.length) return <p className="member-empty">Membership plans are not available right now.</p>
  return <div className="member-plan-grid">{plans.map((plan) => <article className="member-plan-card" key={plan.name}>
    <div><span>{plan.name}</span><strong>₹{Number(plan.price).toLocaleString('en-IN')}</strong><small>{plan.durationMonths} month{plan.durationMonths === 1 ? '' : 's'}</small></div>
    <button type="button" disabled={Boolean(busy)} onClick={() => onChoose(plan.name)}>Proceed to payment</button>
  </article>)}</div>
}

function MembershipList({ items, busy, onCancel, onPay }) {
  return <div className="member-table-scroll"><table><thead><tr><th>Plan</th><th>Price</th><th>Dates</th><th>Status</th><th>Actions</th></tr></thead><tbody>{items.map((item) => {
    const membershipId = item.id ?? item.membershipId
    return <tr key={membershipId ?? `${item.planName}-${item.startDate}`}>
      <td>{item.planName}</td><td>₹{item.price}</td><td>{item.startDate} – {item.endDate}</td><td><span className="member-status">{item.status}</span></td>
      <td><div className="member-row-actions">{item.status === 'PENDING_PAYMENT' && <><button type="button" disabled={Boolean(busy)} onClick={() => onPay(item.planName)}>Pay with Razorpay</button><button type="button" disabled={Boolean(busy) || !membershipId} onClick={() => onCancel(membershipId)}>Remove</button></>}{item.status === 'ACTIVE' && <button type="button" disabled={Boolean(busy) || !membershipId} onClick={() => onCancel(membershipId)}>Cancel</button>}</div></td>
    </tr>
  })}</tbody></table></div>
}

function TrainerList({ trainers }) {
  if (!trainers.length) return <p className="member-empty">No trainer records are available right now.</p>
  return <div className="member-trainer-grid">{trainers.map((trainer) => <article key={trainer.id}><span><Dumbbell size={17} /></span><div><strong>{trainer.name}</strong><small>{trainer.specialization || 'Trainer'} · {trainer.experienceYears ?? 0} years</small></div><em>{trainer.availability || trainer.status || ''}</em></article>)}</div>
}
