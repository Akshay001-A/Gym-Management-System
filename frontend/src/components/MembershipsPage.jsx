import { useEffect, useMemo, useState } from 'react'
import { BadgeCheck, CalendarDays, CreditCard, Eye, Pencil, Plus, Search, Trash2, Users, X } from 'lucide-react'
import { getAllMembers } from '../api/members'
import { assignMembershipPlan, deleteMembership, getMembership, getMemberships, getMembershipPlans, updateMembership } from '../api/memberships'

const PAGE_SIZE = 10

function MembershipForm({ mode, membership, members, plans, initialPlan, error, saving, onClose, onSave }) {
  const [values, setValues] = useState(() => mode === 'edit' ? {
    memberId: membership.memberId,
    planName: membership.planName,
    startDate: membership.startDate,
    endDate: membership.endDate,
    price: membership.price,
  } : { memberId: '', plan: initialPlan || plans[0]?.name || '', planName: plans[0]?.name || '', startDate: '', endDate: '', price: '' })
  const [planNotice, setPlanNotice] = useState('')

  function change(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    if (name === 'plan') {
      const chosen = plans.find((plan) => plan.name === value)
      setValues((current) => ({ ...current, plan: value }))
      setPlanNotice(`The backend will set the start date, ${chosen.duration} end date, and ₹${chosen.price} price when this plan is assigned.`)
    }
  }

  function submit(event) {
    event.preventDefault()
    onSave(mode === 'edit' ? {
      memberId: Number(values.memberId),
      planName: values.planName.trim(),
      startDate: values.startDate,
      endDate: values.endDate,
      price: Number(values.price),
    } : { memberId: Number(values.memberId), plan: values.plan })
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) onClose() }}>
      <section className="member-form" role="dialog" aria-modal="true" aria-labelledby="membership-form-title">
        <div className="modal-heading"><div><h2 id="membership-form-title">{mode === 'edit' ? 'Edit membership' : 'Assign a plan'}</h2><p>{mode === 'edit' ? `Membership #${membership.id}` : 'Choose a member and a plan defined by the backend.'}</p></div><button type="button" className="icon-button" aria-label="Close dialog" onClick={onClose} disabled={saving}><X size={18} /></button></div>
        {error && <div className="member-error" role="alert">{error}</div>}
        <form onSubmit={submit}>
          <div className="member-fields">
            <label>Member<select name="memberId" value={values.memberId} onChange={change} required><option value="">Select a member</option>{members.map((member) => <option value={member.id} key={member.id}>#{member.id} · {member.name}</option>)}</select></label>
            {mode === 'create' ? <label>Plan<select name="plan" value={values.plan} onChange={change}>{plans.map((plan) => <option value={plan.name} key={plan.name}>{plan.name} · {plan.duration} · ₹{plan.price}</option>)}</select></label> : <>
              <label>Plan name<input name="planName" value={values.planName} onChange={change} required /></label>
              <label>Start date<input name="startDate" type="date" value={values.startDate} onChange={change} required /></label>
              <label>End date<input name="endDate" type="date" value={values.endDate} onChange={change} required min={values.startDate || undefined} /></label>
              <label>Price<input name="price" type="number" value={values.price} onChange={change} min="0.01" step="0.01" required /></label>
              <p className="form-note form-note-wide">The backend rejects start dates in the past and end dates before the start date. Status is maintained by the service.</p>
            </>}
          </div>
          {planNotice && <p className="form-note">{planNotice}</p>}
          {members.length === 0 && <p className="member-error">No members are available. Add a member before assigning a plan.</p>}
          <div className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose} disabled={saving}>Cancel</button><button type="submit" className="button button-primary" disabled={saving || members.length === 0}>{saving ? 'Saving…' : mode === 'edit' ? 'Save changes' : 'Assign plan'}</button></div>
        </form>
      </section>
    </div>
  )
}

export default function MembershipsPage({ setNotice }) {
  const [memberships, setMemberships] = useState([])
  const [members, setMembers] = useState([])
  const [plans, setPlans] = useState([])
  const [plansError, setPlansError] = useState('')
  const [error, setError] = useState('')
  const [membersError, setMembersError] = useState('')
  const [loadedKey, setLoadedKey] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState(null)
  const [dialogError, setDialogError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [details, setDetails] = useState(null)

  const loading = loadedKey !== `memberships:${reloadKey}`
  useEffect(() => {
    const controller = new AbortController()
    const load = async () => {
      const [membershipResult, memberResult, planResult] = await Promise.allSettled([
        getMemberships({ signal: controller.signal }),
        getAllMembers({ signal: controller.signal }),
        getMembershipPlans({ signal: controller.signal }),
      ])
      if (controller.signal.aborted) return
      if (membershipResult.status === 'fulfilled') {
        setMemberships(membershipResult.value)
        setError('')
      } else setError(membershipResult.reason.message)
      if (memberResult.status === 'fulfilled') {
        setMembers(memberResult.value)
        setMembersError('')
      } else {
        setMembers([])
        setMembersError(memberResult.reason.message)
      }
      if (planResult.status === 'fulfilled') {
        setPlans(planResult.value.map((plan) => ({
          ...plan,
          duration: plan.durationMonths + ' month' + (plan.durationMonths === 1 ? '' : 's'),
        })))
        setPlansError('')
      } else {
        setPlans([])
        setPlansError(planResult.reason.message)
      }
      setLoadedKey(`memberships:${reloadKey}`)
    }
    load()
    return () => controller.abort()
  }, [reloadKey])

  const memberById = useMemo(() => new Map(members.map((member) => [String(member.id), member])), [members])
  const filtered = useMemo(() => memberships.filter((membership) => {
    const member = memberById.get(String(membership.memberId))
    return `${membership.id} ${membership.memberId} ${member?.name ?? ''} ${membership.planName} ${membership.status}`.toLowerCase().includes(query.trim().toLowerCase())
  }), [memberships, memberById, query])
  const visibleMemberships = useMemo(() => filtered.filter((item) =>
    statusFilter === 'ALL' || item.status?.toUpperCase() === statusFilter), [filtered, statusFilter])
  const pageCount = Math.ceil(visibleMemberships.length / PAGE_SIZE)
  const rows = visibleMemberships.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const activeCount = memberships.filter((item) => item.status?.toUpperCase() === 'ACTIVE').length
  const expiredCount = memberships.filter((item) => item.status?.toUpperCase() === 'EXPIRED').length
  const cancelledCount = memberships.filter((item) => item.status?.toUpperCase() === 'CANCELLED').length

  function openCreate(planName = '') {
    setDialog({ mode: 'create', planName })
    setDialogError('')
  }

  function openEdit(membership) {
    setDialog({ mode: 'edit', membership })
    setDialogError('')
  }

  async function openDetails(membership) {
    setDetails({ loading: true, membership })
    try {
      setDetails({ loading: false, membership: await getMembership(membership.id) })
    } catch (requestError) {
      setDetails({ loading: false, membership, error: requestError.message })
    }
  }

  async function save(payload) {
    if (saving || !dialog) return
    setSaving(true)
    setDialogError('')
    try {
      if (dialog.mode === 'edit') await updateMembership(dialog.membership.id, payload)
      else await assignMembershipPlan(payload.memberId, payload.plan)
      setDialog(null)
      setNotice(dialog.mode === 'edit' ? 'Membership updated.' : 'Membership plan assigned.')
      setReloadKey((key) => key + 1)
    } catch (requestError) {
      setDialogError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  async function confirmDelete() {
    if (!deleteTarget || deleting) return
    setDeleting(true)
    try {
      await deleteMembership(deleteTarget.id)
      setDeleteTarget(null)
      setNotice(`Membership #${deleteTarget.id} deleted.`)
      setReloadKey((key) => key + 1)
    } catch (requestError) {
      setNotice(`Could not delete membership: ${requestError.message}`)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="page-content membership-page-enhanced">
      <div className="page-title-row"><div><p className="eyebrow">MEMBERSHIP MANAGEMENT</p><h1>Memberships</h1><p className="page-subtitle">Manage plans and member subscriptions.</p></div><button className="button button-primary" onClick={() => openCreate()} disabled={!plans.length}><Plus size={17} /> Assign a plan</button></div>
      <section className="membership-summary" aria-label="Membership summary">
        <article><span><CreditCard size={18} /></span><div><small>ALL RECORDS</small><strong>{loading ? '—' : memberships.length}</strong><p>Stored memberships</p></div></article>
        <article><span className="is-active"><BadgeCheck size={18} /></span><div><small>ACTIVE</small><strong>{loading ? '—' : activeCount}</strong><p>Current plan records</p></div></article>
        <article><span className="is-expired"><CalendarDays size={18} /></span><div><small>EXPIRED</small><strong>{loading ? '—' : expiredCount}</strong><p>Ended subscriptions</p></div></article>
        <article><span className="is-cancelled"><X size={18} /></span><div><small>CANCELLED</small><strong>{loading ? '—' : cancelledCount}</strong><p>Cancelled records</p></div></article>
      </section>
      <section className="surface directory-panel membership-records">
        <div className="directory-toolbar"><div><h2>Membership records</h2><p>{filtered.length} records{query ? ' match this search' : ''}</p></div><label className="search-box"><Search size={17} /><input aria-label="Search memberships" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Search member, plan, status, or ID…" />{query && <button type="button" aria-label="Clear search" onClick={() => { setQuery(''); setPage(0) }}><X size={15} /></button>}</label></div>
        <div className="membership-filter-row" role="group" aria-label="Filter memberships by status">{['ALL', 'ACTIVE', 'EXPIRED', 'CANCELLED'].map((status) => <button type="button" key={status} className={statusFilter === status ? 'membership-filter active' : 'membership-filter'} onClick={() => { setStatusFilter(status); setPage(0) }}>{status === 'ALL' ? 'All records' : status.toLowerCase()}</button>)}<span className="filter-spacer" /><span className="record-count"><Users size={14} /> {visibleMemberships.length} results</span></div>
        {membersError && <div className="member-error" role="status">Member options unavailable: {membersError}</div>}
        {error ? <div className="member-error" role="alert">{error}<button type="button" className="button button-secondary" onClick={() => setReloadKey((key) => key + 1)}>Retry</button></div> : loading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading memberships…</div> : visibleMemberships.length === 0 ? <div className="membership-empty"><span><CreditCard size={23} /></span><strong>{memberships.length ? 'No matching memberships' : 'No memberships yet'}</strong><p>{memberships.length ? 'Try another search or status filter.' : 'Assign a plan to create the first record.'}</p>{memberships.length === 0 && <button className="button button-primary" onClick={() => openCreate()} disabled={!plans.length}><Plus size={16} /> Assign a plan</button>}</div> : (
          <div className="table-wrap"><table className="member-table membership-table"><thead><tr><th>ID</th><th>MEMBER</th><th>PLAN</th><th>PRICE</th><th>START</th><th>END</th><th>STATUS</th><th>ACTIONS</th></tr></thead><tbody>{rows.map((membership) => <tr key={membership.id}><td data-label="Membership ID">#{membership.id}</td><td data-label="Member">{memberById.get(String(membership.memberId))?.name ?? `Member #${membership.memberId}`}</td><td data-label="Plan">{membership.planName}</td><td data-label="Price">₹{membership.price}</td><td data-label="Start date">{membership.startDate || '—'}</td><td data-label="End date">{membership.endDate || '—'}</td><td data-label="Status"><span className={'membership-status membership-status--' + (membership.status || 'unknown').toLowerCase()}>{membership.status || 'Unknown'}</span></td><td data-label="Actions"><div className="row-actions"><button type="button" title="View details" aria-label={`View membership ${membership.id}`} onClick={() => openDetails(membership)}><Eye size={15} /></button><button type="button" title="Edit membership" aria-label={`Edit membership ${membership.id}`} onClick={() => openEdit(membership)}><Pencil size={15} /></button><button type="button" title="Delete membership" aria-label={`Delete membership ${membership.id}`} onClick={() => setDeleteTarget(membership)}><Trash2 size={15} /></button></div></td></tr>)}</tbody></table></div>
        )}
        {!loading && !error && pageCount > 1 && <div className="pagination"><span>Page {page + 1} of {pageCount}</span><div><button className="button button-secondary" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0}>Previous</button><button className="button button-secondary" onClick={() => setPage((current) => Math.min(pageCount - 1, current + 1))} disabled={page + 1 >= pageCount}>Next</button></div></div>}
        <p className="panel-footnote"><CreditCard size={13} /> The service defines MONTHLY, QUARTERLY, and YEARLY plans. Plan assignment uses backend dates and prices.</p>
      </section>
      <section className="membership-catalog surface">
        <div className="membership-heading"><div><p className="eyebrow">PLAN OPTIONS</p><h2>Plan catalog</h2><p>Prices and durations are loaded directly from the backend.</p></div></div>
        {plansError && <div className="member-error" role="status">Plans could not be loaded: {plansError}<button type="button" className="button button-secondary" onClick={() => setReloadKey((key) => key + 1)}>Retry</button></div>}
        {loading && !plansError ? <div className="membership-skeletons"><i /><i /><i /></div> : plans.length > 0 && <div className="membership-plans">{plans.map((plan, index) => <article className="membership-plan-card" key={plan.name} style={{ '--plan-order': index }}><div className="plan-card-top"><span className="plan-card-icon"><CreditCard size={17} /></span><small>{plan.duration}</small></div><h3>{plan.name.toLowerCase()}</h3><strong className="plan-price">₹{Number(plan.price).toLocaleString('en-IN', { minimumFractionDigits: 2 })}<small> / plan</small></strong><p>Choose a service-managed plan to assign to a member.</p><button type="button" onClick={() => openCreate(plan.name)} disabled={!members.length}><span>Assign to member</span><Plus size={15} /></button></article>)}</div>}
      </section>
      {dialog && <MembershipForm key={`${dialog.mode}-${dialog.membership?.id ?? 'new'}`} mode={dialog.mode} membership={dialog.membership} members={members} plans={plans} initialPlan={dialog.planName} error={dialogError || (membersError ? 'Could not load members for the assignment list.' : '')} saving={saving} onClose={() => !saving && setDialog(null)} onSave={save} />}
      {details && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setDetails(null)}><section className="member-form" role="dialog" aria-modal="true" aria-labelledby="membership-detail-title"><div className="modal-heading"><div><h2 id="membership-detail-title">Membership details</h2><p>Record #{details.membership.id}</p></div><button type="button" className="icon-button" aria-label="Close details" onClick={() => setDetails(null)}><X size={18} /></button></div>{details.loading ? <div className="loading-state"><span className="loading-spinner" />Loading…</div> : <><dl className="detail-list">{Object.entries(details.membership).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value ?? '—'}</dd></div>)}</dl>{details.error && <div className="member-error" role="alert">{details.error}</div>}</>}</section></div>}
      {deleteTarget && <div className="modal-backdrop"><section className="member-form confirm-dialog" role="alertdialog" aria-modal="true"><div className="modal-heading"><div><h2>Delete membership?</h2><p>Delete membership #{deleteTarget.id}? This removes the record from the Membership Service.</p></div></div><div className="modal-actions"><button className="button button-secondary" onClick={() => setDeleteTarget(null)} disabled={deleting}>Cancel</button><button className="button button-danger" onClick={confirmDelete} disabled={deleting}>{deleting ? 'Deleting…' : 'Delete membership'}</button></div></section></div>}
    </div>
  )
}
