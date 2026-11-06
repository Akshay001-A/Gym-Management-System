import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Clock3, QrCode, RefreshCw } from 'lucide-react'
import { createAttendanceQrToken, getAdminAttendance } from '../api/attendance'

const PAGE_SIZE = 100
const today = () => new Date().toISOString().slice(0, 10)

function duration(seconds) {
  if (seconds == null) return 'In progress'
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const remainingSeconds = safeSeconds % 60
  if (hours) return `${hours}h ${minutes}m ${remainingSeconds}s`
  if (minutes) return `${minutes}m ${remainingSeconds}s`
  return `${remainingSeconds}s`
}

function limitStatus(seconds) {
  if (seconds >= 9000) return { label: 'Over daily maximum', tone: 'over' }
  if (seconds >= 7200) return { label: 'Target reached', tone: 'near' }
  return { label: 'Within target', tone: 'ok' }
}

export default function AttendancePage() {
  const [qr, setQr] = useState(null)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [qrError, setQrError] = useState('')
  const [refreshingQr, setRefreshingQr] = useState(false)
  const qrRequestInFlight = useRef(false)
  const autoRefreshStartedFor = useRef(null)
  const [date, setDate] = useState(today())
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [attendanceLoadedAt, setAttendanceLoadedAt] = useState(Date.now())
  const [expandedMember, setExpandedMember] = useState(null)

  const refreshQr = useCallback(async () => {
    if (qrRequestInFlight.current) return
    qrRequestInFlight.current = true
    setRefreshingQr(true)
    setQrError('')
    try {
      const value = await createAttendanceQrToken()
      setQr(value)
    } catch (requestError) { setQrError(requestError.message) }
    finally {
      qrRequestInFlight.current = false
      setRefreshingQr(false)
    }
  }, [])

  useEffect(() => { refreshQr() }, [refreshQr])
  useEffect(() => {
    if (!qr?.expiresAt) return undefined
    const update = () => {
      const left = Math.max(0, Math.ceil((new Date(qr.expiresAt).getTime() - Date.now()) / 1000))
      setSecondsLeft(left)
      if (left <= 15 && autoRefreshStartedFor.current !== qr.qrPayload) {
        autoRefreshStartedFor.current = qr.qrPayload
        refreshQr()
      }
    }
    update()
    const timer = window.setInterval(update, 1000)
    return () => window.clearInterval(timer)
  }, [qr, refreshQr])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    async function loadAttendance() {
      try {
        const firstPage = await getAdminAttendance({ date, page: 0, size: PAGE_SIZE, signal: controller.signal })
        const remainingPages = await Promise.all(Array.from(
          { length: Math.max(0, firstPage.totalPages - 1) },
          (_, index) => getAdminAttendance({ date, page: index + 1, size: PAGE_SIZE, signal: controller.signal }),
        ))
        setResult({ ...firstPage, content: [firstPage, ...remainingPages].flatMap((page) => page.content) })
        setAttendanceLoadedAt(Date.now())
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setError(requestError.message)
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    loadAttendance()
    return () => controller.abort()
  }, [date, reload])

  const rows = result?.content ?? []
  const memberTotals = useMemo(() => {
    const grouped = new Map()
    rows.forEach((visit) => {
      const key = visit.memberId ?? visit.memberName
      const member = grouped.get(key) ?? {
        memberId: visit.memberId, name: visit.memberName || `Member #${visit.memberId}`,
        totalSeconds: 0, visitCount: 0, visits: [],
      }
      member.totalSeconds += (visit.durationSeconds ?? 0) + (visit.currentlyInside ? Math.max(0, Math.floor((Date.now() - attendanceLoadedAt) / 1000)) : 0)
      member.visits.push(visit)
      member.visitCount += 1
      grouped.set(key, member)
    })
    return [...grouped.values()].sort((left, right) => right.totalSeconds - left.totalSeconds)
  }, [rows, attendanceLoadedAt, secondsLeft])
  return <div className="page-content attendance-page">
    <div className="page-title-row"><div><p className="eyebrow">CLUB OPERATIONS</p><h1>Attendance</h1><p className="page-subtitle">Members scan this code at the entrance to check in and check out.</p></div></div>
    <div className="attendance-admin-grid">
      <section className="surface attendance-qr-card"><div className="attendance-heading"><span><QrCode size={19} /></span><div><h2>Gym entrance QR</h2><p>Refreshes automatically before it expires.</p></div></div>
        {qr?.qrImageDataUrl ? <img className="attendance-qr-image" src={qr.qrImageDataUrl} alt="Current gym attendance QR code" /> : <div className="attendance-qr-placeholder"><QrCode size={44} />{qrError || 'Creating secure QR code…'}</div>}
        <div className="attendance-qr-footer"><span><Clock3 size={15} /> {secondsLeft ? `Refreshes in ${secondsLeft}s` : 'Preparing next code'}</span><button className="button button-secondary" type="button" onClick={refreshQr}><RefreshCw size={15} /> Refresh</button></div>
        {qrError && qr?.qrImageDataUrl && <p className="attendance-inline-error" role="alert">Could not refresh: {qrError}</p>}
      </section>
      <section className="surface attendance-list-card"><div className="directory-toolbar"><div><h2>Member attendance</h2><p>{result?.totalElements ?? rows.length} visits · {memberTotals.length} members for the selected day</p></div><label className="attendance-date">Date<input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label></div>
        {error ? <div className="member-error" role="alert">{error}<button className="button button-secondary" onClick={() => { setError(''); setReload((value) => value + 1) }}>Retry</button></div> : loading ? <div className="loading-state">Loading attendance…</div> : memberTotals.length ? <div className="member-table-scroll"><table><thead><tr><th>Member</th><th>Visits</th><th>Total time</th><th>Daily usage</th><th>Status</th><th>Details</th></tr></thead><tbody>{memberTotals.map((member) => {
          const status = limitStatus(member.totalSeconds)
          const usage = Math.min(100, member.totalSeconds / 9000 * 100)
          const isExpanded = expandedMember === member.memberId
          return <Fragment key={member.memberId ?? member.name}><tr><td><strong className="attendance-member-name">{member.name}</strong></td><td>{member.visitCount}</td><td><strong>{duration(member.totalSeconds)}</strong></td><td><div className="attendance-limit-cell"><div className="attendance-limit-track"><span className={status.tone} style={{ width: `${usage}%` }} /><i /></div><small>{duration(member.totalSeconds)} / 2h 30m</small></div></td><td><span className={`attendance-limit-status ${status.tone}`}>{status.label}</span></td><td><button className="attendance-detail-button" type="button" onClick={() => setExpandedMember(isExpanded ? null : member.memberId)}>{isExpanded ? 'Hide visits' : 'View visits'}</button></td></tr>
            {isExpanded && <tr className="attendance-expanded-row"><td colSpan="6"><div className="attendance-expanded-content"><h3>Visits for {member.name}</h3><div className="member-table-scroll"><table><thead><tr><th>Arrived</th><th>Left</th><th>Time inside</th><th>Status</th></tr></thead><tbody>{member.visits.map((visit) => <tr key={visit.visitId}><td>{visit.checkInAt ? new Date(visit.checkInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'}</td><td>{visit.checkOutAt ? new Date(visit.checkOutAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'}</td><td>{duration((visit.durationSeconds ?? 0) + (visit.currentlyInside ? Math.max(0, Math.floor((Date.now() - attendanceLoadedAt) / 1000)) : 0))}</td><td>{visit.currentlyInside ? 'Inside now' : 'Complete'}</td></tr>)}</tbody></table></div></div></td></tr>}</Fragment>
        })}</tbody></table></div> : <div className="attendance-empty">No visits recorded for this date.</div>}
      </section>
    </div>
  </div>
}
