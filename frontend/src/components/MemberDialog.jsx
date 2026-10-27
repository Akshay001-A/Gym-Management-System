import { useState } from 'react'
import { Pencil, Plus, X } from 'lucide-react'

const emptyMember = {
  name: '', email: '', age: '', mobile: '', gymTime: '', gender: '', fatherName: '', amount: '',
}

function toFormValues(member) {
  return Object.fromEntries(Object.keys(emptyMember).map((key) => [key, member?.[key] ?? '']))
}

export default function MemberDialog({ mode, member, loading = false, saving = false, error = '', onClose, onSave }) {
  const [values, setValues] = useState(() => toFormValues(member))

  const isDetails = mode === 'details'
  const title = isDetails ? 'Member details' : mode === 'edit' ? 'Edit member' : 'Add a member'

  function change(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function submit(event) {
    event.preventDefault()
    onSave({
      name: values.name.trim(),
      email: values.email.trim() || null,
      age: values.age === '' ? null : Number(values.age),
      mobile: values.mobile.trim() || null,
      gymTime: values.gymTime.trim() || null,
      gender: values.gender.trim() || null,
      fatherName: values.fatherName.trim() || null,
      amount: values.amount === '' ? null : Number(values.amount),
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
              <label>Email address<input name="email" type="email" value={values.email} onChange={change} readOnly={isDetails} /></label>
              <label>Age<input name="age" type="number" min="1" step="1" value={values.age} onChange={change} readOnly={isDetails} /></label>
              <label>Mobile number<input name="mobile" inputMode="tel" value={values.mobile} onChange={change} readOnly={isDetails} /></label>
              <label>Preferred gym time<input name="gymTime" value={values.gymTime} onChange={change} readOnly={isDetails} /></label>
              <label>Gender<input name="gender" value={values.gender} onChange={change} readOnly={isDetails} /></label>
              <label>Father's name<input name="fatherName" value={values.fatherName} onChange={change} readOnly={isDetails} /></label>
              <label>Amount<input name="amount" type="number" min="0" step="0.01" value={values.amount} onChange={change} readOnly={isDetails} /></label>
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
