import { useEffect, useState } from 'react'
import { Pencil, Plus, X } from 'lucide-react'
import { getMembershipPlans } from '../api/member'

const emptyMember = {
  name: '', email: '', age: '', mobile: '', gymTime: 'Morning', gender: '', fatherName: '', amount: '', plan: 'MONTHLY',
}

function toFormValues(member) {
  return Object.fromEntries(Object.keys(emptyMember).map((key) => [key, member?.[key] ?? (key === 'gymTime' ? 'Morning' : '')]))
}

export default function MemberDialog({ mode, member, loading = false, saving = false, error = '', onClose, onSave }) {
  const [values, setValues] = useState(() => toFormValues(member))
  const [plans, setPlans] = useState([])
  const [plansError, setPlansError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    getMembershipPlans({ signal: controller.signal })
      .then(setPlans)
      .catch((requestError) => { if (!controller.signal.aborted) setPlansError(requestError.message) })
    return () => controller.abort()
  }, [])

  const isDetails = mode === 'details'
  const title = isDetails ? 'Member details' : mode === 'edit' ? 'Edit member' : 'Add a member'

  function change(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function submit(event) {
    event.preventDefault()
    const selectedPlan = plans.find((plan) => plan.name === values.plan)
    onSave({
      name: values.name.trim(),
      email: values.email.trim() || null,
      age: values.age === '' ? null : Number(values.age),
      mobile: values.mobile.trim() || null,
      gymTime: values.gymTime.trim() || null,
      gender: values.gender.trim() || null,
      fatherName: values.fatherName.trim() || null,
      amount: mode === 'create' ? (selectedPlan ? Number(selectedPlan.price) : null) : (values.amount === '' ? null : Number(values.amount)),
      ...(mode === 'create' ? { plan: values.plan } : {}),
    })
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) onClose() }}>
      <section className="member-form" role="dialog" aria-modal="true" aria-labelledby="member-dialog-title">
        <div className="modal-heading">
          <div><h2 id="member-dialog-title">{title}</h2><p>{isDetails ? `Record #${member?.id ?? ''}` : 'Enter the member information.'}</p></div>
          <button type="button" className="icon-button" aria-label="Close dialog" onClick={onClose} disabled={saving}><X size={18} /></button>
        </div>

        {loading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading member details…</div> : (
          <form onSubmit={submit}>
            {error && <div className="member-error" role="alert">{error}</div>}
            <div className="member-fields">
              <label>Full name<input name="name" required maxLength="255" value={values.name} onChange={change} readOnly={isDetails} autoFocus={!isDetails} /></label>
              <label>Email address<input name="email" type="email" value={values.email} onChange={change} readOnly={isDetails} required={!isDetails} /></label>
              <label>Age<input name="age" type="number" min="1" step="1" value={values.age} onChange={change} readOnly={isDetails} /></label>
              <label>Mobile number<input name="mobile" inputMode="numeric" pattern="[0-9]{10}" maxLength="10" title="Enter the 10-digit mobile number used for member verification" value={values.mobile} onChange={change} readOnly={isDetails} required={!isDetails} /></label>
              <label>Preferred gym time<select name="gymTime" value={values.gymTime} onChange={change} disabled={isDetails} required={!isDetails}><option value="">Select a time</option><option>Morning</option><option>Afternoon</option><option>Evening</option></select></label>
              <label>Gender<input name="gender" value={values.gender} onChange={change} readOnly={isDetails} /></label>
              <label>Father's name<input name="fatherName" value={values.fatherName} onChange={change} readOnly={isDetails} /></label>
              {mode === 'create' ? <label>Membership plan<select name="plan" value={values.plan} onChange={change} required><option value="">Select a membership plan</option>{plans.map((plan) => <option key={plan.name} value={plan.name}>{plan.name} — ₹{Number(plan.price).toLocaleString('en-IN')} / {plan.durationMonths} month{plan.durationMonths === 1 ? '' : 's'}</option>)}</select>{plansError && <small className="field-help" role="status">Plan choices could not load: {plansError}</small>}<small className="field-help">The member can pay for this assigned plan after signing in.</small></label> : <label>Membership amount<select name="amount" value={values.amount} onChange={change} disabled={isDetails} required={!isDetails}><option value="">Select a membership plan</option>{plans.map((plan) => <option key={plan.name} value={Number(plan.price)}>{plan.name} — ₹{Number(plan.price).toLocaleString('en-IN')}</option>)}{values.amount && !plans.some((plan) => Number(plan.price) === Number(values.amount)) && <option value={values.amount}>Current amount — ₹{Number(values.amount).toLocaleString('en-IN')}</option>}</select>{plansError && <small className="field-help" role="status">Plan prices could not load: {plansError}</small>}</label>}
            </div>
            {isDetails ? <div className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose}>Close</button></div> : (
              <div className="modal-actions">
                <button type="button" className="button button-secondary" onClick={onClose} disabled={saving}>Cancel</button>
                <button type="submit" className="button button-primary" disabled={saving}>{mode === 'edit' ? <Pencil size={16} /> : <Plus size={16} />}{saving ? 'Saving…' : mode === 'edit' ? 'Save changes' : 'Add member'}</button>
              </div>
            )}
          </form>
        )}
      </section>
    </div>
  )
}
