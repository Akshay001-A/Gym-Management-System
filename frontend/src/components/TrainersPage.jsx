import { useEffect, useMemo, useState } from 'react'
import { Dumbbell, Eye, Pencil, Plus, Search, Trash2, X } from 'lucide-react'
import { createTrainer, deleteTrainer, getTrainer, getTrainers, updateTrainer } from '../api/trainers'

const emptyTrainer = { name: '', email: '', mobile: '', specialization: '', experienceYears: '', availability: 'AVAILABLE', status: 'ACTIVE' }

function TrainerForm({ mode, trainer, error, saving, onClose, onSave }) {
  const [values, setValues] = useState(() => mode === 'edit' ? { ...trainer } : { ...emptyTrainer })

  function change(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function submit(event) {
    event.preventDefault()
    onSave({
      name: values.name.trim(),
      email: values.email.trim(),
      mobile: values.mobile.trim(),
      specialization: values.specialization.trim(),
      experienceYears: Number(values.experienceYears),
      availability: values.availability.trim(),
      status: values.status.trim(),
    })
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) onClose() }}>
      <section className="member-form" role="dialog" aria-modal="true" aria-labelledby="trainer-form-title">
        <div className="modal-heading"><div><h2 id="trainer-form-title">{mode === 'edit' ? 'Edit trainer' : 'Add a trainer'}</h2><p>Enter details required by the Trainer Service.</p></div><button type="button" className="icon-button" aria-label="Close dialog" onClick={onClose} disabled={saving}><X size={18} /></button></div>
        {error && <div className="member-error" role="alert">{error}</div>}
        <form onSubmit={submit}><div className="member-fields">
          <label>Full name<input name="name" value={values.name} onChange={change} required maxLength="100" /></label>
          <label>Email address<input name="email" type="email" value={values.email} onChange={change} required /></label>
          <label>Mobile number<input name="mobile" value={values.mobile} onChange={change} required inputMode="numeric" pattern="[0-9]{10}" title="Enter exactly 10 digits" /></label>
          <label>Specialization<input name="specialization" value={values.specialization} onChange={change} required /></label>
          <label>Experience in years<input name="experienceYears" type="number" min="0" max="60" step="1" value={values.experienceYears} onChange={change} required /></label>
          <label>Availability<input name="availability" value={values.availability} onChange={change} required placeholder="AVAILABLE" /></label>
          <label>Status<input name="status" value={values.status} onChange={change} required placeholder="ACTIVE" /></label>
        </div><div className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose} disabled={saving}>Cancel</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? 'Saving…' : mode === 'edit' ? 'Save changes' : 'Add trainer'}</button></div></form>
      </section>
    </div>
  )
}

export default function TrainersPage({ setNotice }) {
  const [trainers, setTrainers] = useState([])
  const [error, setError] = useState('')
  const [loadedKey, setLoadedKey] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [query, setQuery] = useState('')
  const [availability, setAvailability] = useState('')
  const [status, setStatus] = useState('')
  const [dialog, setDialog] = useState(null)
  const [dialogError, setDialogError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [details, setDetails] = useState(null)
  const loading = loadedKey !== `trainers:${reloadKey}`

  useEffect(() => {
    const controller = new AbortController()
    getTrainers({ signal: controller.signal })
      .then((result) => { setTrainers(result); setError('') })
      .catch((requestError) => { if (!controller.signal.aborted) setError(requestError.message) })
      .finally(() => { if (!controller.signal.aborted) setLoadedKey(`trainers:${reloadKey}`) })
    return () => controller.abort()
  }, [reloadKey])

  const filtered = useMemo(() => trainers.filter((trainer) => {
    const textMatch = `${trainer.id} ${trainer.name} ${trainer.email} ${trainer.mobile} ${trainer.specialization}`.toLowerCase().includes(query.trim().toLowerCase())
    return textMatch && (!availability || trainer.availability?.toLowerCase() === availability.toLowerCase()) && (!status || trainer.status?.toLowerCase() === status.toLowerCase())
  }), [trainers, query, availability, status])

  function openCreate() {
    setDialog({ mode: 'create' })
    setDialogError('')
  }

  async function openDetails(trainer) {
    setDetails({ loading: true, trainer })
    try {
      setDetails({ loading: false, trainer: await getTrainer(trainer.id) })
    } catch (requestError) {
      setDetails({ loading: false, trainer, error: requestError.message })
    }
  }

  async function save(payload) {
    if (saving || !dialog) return
    setSaving(true)
    setDialogError('')
    try {
      if (dialog.mode === 'edit') await updateTrainer(dialog.trainer.id, payload)
      else await createTrainer(payload)
      setDialog(null)
      setNotice(dialog.mode === 'edit' ? 'Trainer updated.' : 'Trainer added.')
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
      await deleteTrainer(deleteTarget.id)
      setDeleteTarget(null)
      setNotice(`Trainer ${deleteTarget.name} deleted.`)
      setReloadKey((key) => key + 1)
    } catch (requestError) {
      setNotice(`Could not delete trainer: ${requestError.message}`)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="page-content">
      <div className="page-title-row"><div><p className="eyebrow">COACHING TEAM</p><h1>Trainers</h1><p className="page-subtitle">Manage trainer profiles from the Trainer Service.</p></div><button className="button button-primary" onClick={openCreate}><Plus size={17} /> Add trainer</button></div>
      <section className="surface directory-panel">
        <div className="directory-toolbar"><div><h2>Training team</h2><p>{filtered.length} of {trainers.length} trainer records</p></div><div className="directory-filters"><label className="search-box"><Search size={17} /><input aria-label="Search trainers" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search name, email, or specialization…" />{query && <button type="button" aria-label="Clear search" onClick={() => setQuery('')}><X size={15} /></button>}</label><input className="filter-input" aria-label="Filter by availability" placeholder="Availability" value={availability} onChange={(event) => setAvailability(event.target.value)} /><input className="filter-input" aria-label="Filter by status" placeholder="Status" value={status} onChange={(event) => setStatus(event.target.value)} /></div></div>
        {error ? <div className="member-error" role="alert">{error}<button className="button button-secondary" onClick={() => setReloadKey((key) => key + 1)}>Retry</button></div> : loading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading trainers…</div> : trainers.length === 0 ? <div className="table-empty"><Dumbbell size={24} /><strong>No trainer records</strong><p>Add a trainer to get started.</p></div> : filtered.length === 0 ? <div className="table-empty"><strong>No trainers match these filters</strong><p>Try a different search or filter.</p></div> : <div className="table-wrap"><table className="member-table trainer-table"><thead><tr><th>TRAINER</th><th>MOBILE</th><th>SPECIALIZATION</th><th>EXPERIENCE</th><th>AVAILABILITY</th><th>STATUS</th><th>ACTIONS</th></tr></thead><tbody>{filtered.map((trainer) => <tr key={trainer.id}><td data-label="Trainer"><div className="member-cell"><span className="member-avatar"><Dumbbell size={15} /></span><span><strong>{trainer.name}</strong><small>{trainer.email}</small></span></div></td><td data-label="Mobile">{trainer.mobile}</td><td data-label="Specialization">{trainer.specialization}</td><td data-label="Experience">{trainer.experienceYears} years</td><td data-label="Availability">{trainer.availability}</td><td data-label="Status">{trainer.status}</td><td data-label="Actions"><div className="row-actions"><button type="button" title="View details" aria-label={`View ${trainer.name}`} onClick={() => openDetails(trainer)}><Eye size={15} /></button><button type="button" title="Edit trainer" aria-label={`Edit ${trainer.name}`} onClick={() => { setDialog({ mode: 'edit', trainer }); setDialogError('') }}><Pencil size={15} /></button><button type="button" title="Delete trainer" aria-label={`Delete ${trainer.name}`} onClick={() => setDeleteTarget(trainer)}><Trash2 size={15} /></button></div></td></tr>)}</tbody></table></div>}
        <p className="panel-footnote">Filters apply to the trainer records returned by the service.</p>
      </section>
      {dialog && <TrainerForm key={`${dialog.mode}-${dialog.trainer?.id ?? 'new'}`} mode={dialog.mode} trainer={dialog.trainer} error={dialogError} saving={saving} onClose={() => !saving && setDialog(null)} onSave={save} />}
      {details && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setDetails(null)}><section className="member-form" role="dialog" aria-modal="true" aria-labelledby="trainer-detail-title"><div className="modal-heading"><div><h2 id="trainer-detail-title">Trainer details</h2><p>{details.trainer.name}</p></div><button type="button" className="icon-button" aria-label="Close details" onClick={() => setDetails(null)}><X size={18} /></button></div>{details.loading ? <div className="loading-state"><span className="loading-spinner" />Loading…</div> : <><dl className="detail-list">{Object.entries(details.trainer).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value ?? '—'}</dd></div>)}</dl>{details.error && <div className="member-error" role="alert">{details.error}</div>}</>}</section></div>}
      {deleteTarget && <div className="modal-backdrop"><section className="member-form confirm-dialog" role="alertdialog" aria-modal="true"><div className="modal-heading"><div><h2>Delete trainer?</h2><p>Delete {deleteTarget.name} from the Trainer Service? This cannot be undone.</p></div></div><div className="modal-actions"><button className="button button-secondary" onClick={() => setDeleteTarget(null)} disabled={deleting}>Cancel</button><button className="button button-danger" onClick={confirmDelete} disabled={deleting}>{deleting ? 'Deleting…' : 'Delete trainer'}</button></div></section></div>}
    </div>
  )
}
