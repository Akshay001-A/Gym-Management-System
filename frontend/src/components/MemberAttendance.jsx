import { useCallback, useEffect, useRef, useState } from 'react'
import { Camera, Clock3, RefreshCw, ScanLine } from 'lucide-react'
import { getMyAttendance, scanAttendanceQr } from '../api/attendance'

function duration(seconds = 0) {
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const remainingSeconds = safeSeconds % 60
  if (hours) return `${hours}h ${minutes}m ${remainingSeconds}s`
  if (minutes) return `${minutes}m ${remainingSeconds}s`
  return `${remainingSeconds}s`
}

function gymDate(date = new Date()) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date)
}

function clockTime(value) {
  return value ? new Date(value).toLocaleTimeString([], {
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  }) : '—'
}

function limitStatus(seconds) {
  if (seconds >= 9000) return { label: 'Above 2h 30m maximum', tone: 'over' }
  if (seconds >= 7200) return { label: 'Past 2h target', tone: 'near' }
  return { label: 'Within 2h target', tone: 'ok' }
}

function explainAttendanceError(error) {
  if (error.status === 403) {
    return 'The API Gateway is blocking member attendance access. Its security rules currently allow these member routes only for admins. The gateway needs to allow member access to their own attendance routes.'
  }
  return error.message
}

function CameraScanner({ onScan, onClose }) {
  const videoRef = useRef(null)
  const controlsRef = useRef(null)
  const onScanRef = useRef(onScan)
  const scanHandledRef = useRef(false)
  const [cameraError, setCameraError] = useState('')
  useEffect(() => { onScanRef.current = onScan }, [onScan])
  useEffect(() => {
    let active = true
    async function start() {
      try {
        if (!window.isSecureContext) {
          throw new Error('Camera access requires HTTPS. Open the site on this computer using localhost, or use a secure HTTPS address.')
        }
        if (!navigator.mediaDevices?.getUserMedia) {
          throw new Error('This browser does not provide camera access. Try a current version of Chrome, Edge, Firefox, or Safari.')
        }
        const { BrowserQRCodeReader } = await import('@zxing/browser')
        if (!active) return
        const reader = new BrowserQRCodeReader()
        const controls = await reader.decodeFromVideoDevice(undefined, videoRef.current, (result) => {
          if (!result || scanHandledRef.current) return
          scanHandledRef.current = true
          controlsRef.current?.stop()
          onScanRef.current(result.getText())
        })
        if (active) controlsRef.current = controls
        else controls.stop()
      } catch (error) {
        if (!active) return
        if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          setCameraError('Camera permission was blocked. Allow camera access for this site in your browser settings, then try again.')
        } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
          setCameraError('No camera was found on this device.')
        } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
          setCameraError('The camera is already being used by another app. Close that app and try again.')
        } else {
          setCameraError(error.message || 'Could not start the camera. Check the browser camera permission and try again.')
        }
      }
    }
    start()
    return () => {
      active = false
      controlsRef.current?.stop()
    }
  }, [])
  return <div className="member-camera-panel"><video ref={videoRef} muted playsInline /><p><Camera size={16} /> Point the camera at the gym QR code.</p>{cameraError && <p className="attendance-inline-error" role="status">{cameraError}</p>}<button type="button" className="member-text-button" onClick={onClose}>Close camera</button></div>
}

export default function MemberAttendance() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [cameraOpen, setCameraOpen] = useState(false)
  const [now, setNow] = useState(Date.now())
  const [dataLoadedAt, setDataLoadedAt] = useState(Date.now())
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const result = await getMyAttendance()
      const receivedAt = Date.now()
      setData(result)
      setDataLoadedAt(receivedAt)
      setNow(receivedAt)
    } catch (requestError) { setError(explainAttendanceError(requestError)) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])
  useEffect(() => {
    if (!data?.currentlyInside) return undefined
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [data?.currentlyInside])
  const handleScan = useCallback(async (payload) => {
    if (!payload || busy) return
    setCameraOpen(false)
    setBusy(true); setMessage(''); setError('')
    try {
      const result = await scanAttendanceQr(payload.trim())
      setMessage(result.action === 'CHECKED_IN' ? 'Checked in. Have a good workout!' : 'Checked out. Your visit time has been saved.')
      setCameraOpen(false); await load()
    } catch (requestError) { setError(explainAttendanceError(requestError)) }
    finally { setBusy(false) }
  }, [busy, load])
  const visits = data?.visits ?? []
  const liveDeltaSeconds = data?.currentlyInside ? Math.max(0, Math.floor((now - dataLoadedAt) / 1000)) : 0
  const todaySummary = data?.days?.find((day) => day.date === gymDate())
  const todayTotalSeconds = (todaySummary?.totalDurationSeconds ?? 0) + liveDeltaSeconds
  const dailySummaries = (data?.days ?? []).filter((day) => day.visitCount > 0 || day.totalDurationSeconds > 0).slice().reverse()
  const totalRangeSeconds = (data?.totalDurationSeconds ?? 0) + liveDeltaSeconds
  const todayLimit = limitStatus(todayTotalSeconds)
  const todayUsage = Math.min(100, todayTotalSeconds / 9000 * 100)
  return <section className="member-panel member-attendance-panel">
    <div className="member-section-heading"><div><h2>Gym attendance</h2><p>Scan the entrance QR when you arrive and scan again when you leave.</p></div><button className="member-profile-edit-button" type="button" onClick={load}><RefreshCw size={15} /> Refresh</button></div>
    {data && <div className="attendance-member-summary"><article><span><Clock3 size={17} /></span><small>TODAY’S TOTAL</small><strong>{duration(todayTotalSeconds)}</strong></article><article><span><ScanLine size={17} /></span><small>VISIT STATUS</small><strong>{data.currentlyInside ? 'Checked in' : 'Checked out'}</strong></article>{data.currentCheckInAt && <article><span><Camera size={17} /></span><small>CHECKED IN AT</small><strong>{clockTime(data.currentCheckInAt)}</strong></article>}</div>}
    {data && <section className="attendance-daily-limit"><div className="attendance-limit-heading"><div><h3>Daily attendance guide</h3><p>Recommended target: 2 hours · Maximum guide: 2 hours 30 minutes</p></div><span className={`attendance-limit-status ${todayLimit.tone}`}>{todayLimit.label}</span></div><div className="attendance-limit-readout"><strong>{duration(todayTotalSeconds)}</strong><span>of 2h 30m</span></div><div className="attendance-limit-track attendance-limit-track-large" role="progressbar" aria-label="Today's gym time against the 2 hour 30 minute daily maximum" aria-valuemin="0" aria-valuemax="9000" aria-valuenow={Math.min(todayTotalSeconds, 9000)}><span className={todayLimit.tone} style={{ width: `${todayUsage}%` }} /><i /></div><div className="attendance-limit-scale"><span>0</span><span>2h target</span><span>2h 30m maximum</span></div></section>}
    <div className="attendance-member-actions"><button type="button" onClick={() => setCameraOpen((open) => !open)} disabled={busy}><Camera size={17} /> {cameraOpen ? 'Camera open' : 'Scan gym QR'}</button></div>
    {cameraOpen && <CameraScanner onScan={handleScan} onClose={() => setCameraOpen(false)} />}
    {message && <div className="attendance-success" role="status">{message}</div>}{error && <div className="member-error" role="alert">{error}</div>}
    <div className="attendance-history-heading"><h3>Recent visits</h3><span>Last 30 days</span></div>
    {loading ? <div className="member-loading">Loading your attendance…</div> : visits.length ? <div className="member-table-scroll"><table><thead><tr><th>Date</th><th>Arrived</th><th>Left</th><th>Time inside</th><th>Status</th></tr></thead><tbody>{visits.map((visit) => <tr key={visit.visitId}><td>{new Date(visit.checkInAt).toLocaleDateString()}</td><td>{clockTime(visit.checkInAt)}</td><td>{clockTime(visit.checkOutAt)}</td><td>{duration(visit.durationSeconds + (visit.currentlyInside ? liveDeltaSeconds : 0))}</td><td>{visit.currentlyInside ? 'Inside now' : 'Complete'}</td></tr>)}</tbody></table></div> : <p className="member-empty">No gym visits yet. Scan the QR at the entrance to start your first visit.</p>}
    {!loading && dailySummaries.length > 0 && <section className="attendance-daily-totals"><div className="attendance-history-heading"><h3>Daily totals</h3><span>Total for each day · last 30 days</span></div><div className="member-table-scroll"><table><thead><tr><th>Date</th><th>Visits</th><th>Total time</th><th>Daily limit</th></tr></thead><tbody>{dailySummaries.map((day) => {
      const daySeconds = day.totalDurationSeconds + (data.currentlyInside && day.date === gymDate() ? liveDeltaSeconds : 0)
      const status = limitStatus(daySeconds)
      return <tr key={day.date}><td>{new Date(`${day.date}T00:00:00`).toLocaleDateString()}</td><td>{day.visitCount}</td><td>{duration(daySeconds)}</td><td><span className={`attendance-limit-status ${status.tone}`}>{status.label}</span></td></tr>
    })}<tr className="attendance-total-row"><td>Last 30 days total</td><td>—</td><td>{duration(totalRangeSeconds)}</td><td>—</td></tr></tbody></table></div></section>}
  </section>
}
