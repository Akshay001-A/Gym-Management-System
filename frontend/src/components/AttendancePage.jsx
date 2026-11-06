import { useCallback, useEffect, useState } from 'react'
import { Clock3, QrCode, RefreshCw } from 'lucide-react'
import { createAttendanceQrToken, getAdminAttendance } from '../api/attendance'

const PAGE_SIZE = 50
const today = () => new Date().toISOString().slice(0, 10)

function duration(seconds) {
  if (seconds == null) return 'In progress'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours ? `${hours}h ${minutes}m` : `${minutes}m`
}

export default function AttendancePage() {
  const [qr, setQr] = useState(null)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [qrError, setQrError] = useState('')
  const [refreshingQr, setRefreshingQr] = useState(false)
  const [date, setDate] = useState(today())
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)

  const refreshQr = useCallback(async () => {
    if (refreshingQr) return
    setRefreshingQr(true)
    setQrError('')
    try {
      const value = await createAttendanceQrToken()
      setQr(value)
    } catch (requestError) { setQrError(requestError.message) }
    finally { setRefreshingQr(false) }
  }, [refreshingQr])

  useEffect(() => { refreshQr() }, [refreshQr])
  useEffect(() => {
    if (!qr?.expiresAt) return undefined
    const update = () => {
      const left = Math.max(0, Math.ceil((new Date(qr.expiresAt).getTime() - Date.now()) / 1000))
      setSecondsLeft(left)
      if (left <= 15 && !refreshingQr) refreshQr()
    }
    update()
    const timer = window.setInterval(update, 1000)
    return () => window.clearInterval(timer)
  }, [qr, refreshQr, refreshingQr])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    getAdminAttendance({ date, page: 0, size: PAGE_SIZE, signal: controller.signal })
      .then(setResult).catch((requestError) => { if (requestError.name !== 'AbortError') setError(requestError.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [date, reload])

  const rows = result?.content ?? []
  return <div className="page-content attendance-page">
    <div className="page-title-row"><div><p className="eyebrow">CLUB OPERATIONS</p><h1>Attendance</h1><p className="page-subtitle">Members scan this code at the entrance to check in and check out.</p></div></div>
    <div className="attendance-admin-grid">
      <section className="surface attendance-qr-card"><div className="attendance-heading"><span><QrCode size={19} /></span><div><h2>Gym entrance QR</h2><p>Refreshes automatically before it expires.</p></div></div>
        {qr?.qrImageDataUrl ? <img className="attendance-qr-image" src={qr.qrImageDataUrl} alt="Current gym attendance QR code" /> : <div className="attendance-qr-placeholder"><QrCode size={44} />{qrError || 'Creating secure QR code…'}</div>}
        <div className="attendance-qr-footer"><span><Clock3 size={15} /> {secondsLeft ? `Refreshes in ${secondsLeft}s` : 'Preparing next code'}</span><button className="button button-secondary" type="button" onClick={refreshQr}><RefreshCw size={15} /> Refresh</button></div>
        {qrError && qr?.qrImageDataUrl && <p className="attendance-inline-error" role="alert">Could not refresh: {qrError}</p>}
      </section>
      <section className="surface attendance-list-card"><div className="directory-toolbar"><div><h2>Gym visits</h2><p>{result?.totalElements ?? rows.length} records for the selected day</p></div><label className="attendance-date">Date<input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label></div>
        {error ? <div className="member-error" role="alert">{error}<button className="button button-secondary" onClick={() => { setError(''); setReload((value) => value + 1) }}>Retry</button></div> : loading ? <div className="loading-state">Loading attendance…</div> : rows.length ? <div className="member-table-scroll"><table><thead><tr><th>Member</th><th>Check-in</th><th>Check-out</th><th>Time inside</th><th>Status</th></tr></thead><tbody>{rows.map((visit) => <tr key={visit.visitId}><td>{visit.memberName || `Member #${visit.memberId}`}</td><td>{visit.checkInAt ? new Date(visit.checkInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</td><td>{visit.checkOutAt ? new Date(visit.checkOutAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</td><td>{duration(visit.durationSeconds)}</td><td><span className={`attendance-status ${visit.currentlyInside ? 'inside' : ''}`}>{visit.currentlyInside ? 'Inside now' : 'Checked out'}</span></td></tr>)}</tbody></table></div> : <div className="attendance-empty">No visits recorded for this date.</div>}
      </section>
    </div>
  </div>
}
