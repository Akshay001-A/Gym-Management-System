import { useCallback, useEffect, useRef, useState } from 'react'
import { Camera, Clock3, RefreshCw, ScanLine } from 'lucide-react'
import { getMyAttendance, scanAttendanceQr } from '../api/attendance'

function duration(seconds = 0) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours ? `${hours}h ${minutes}m` : `${minutes}m`
}

function explainAttendanceError(error) {
  if (error.status === 403) {
    return 'The API Gateway is blocking member attendance access. Its security rules currently allow these member routes only for admins. The gateway needs to allow member access to their own attendance routes.'
  }
  return error.message
}

function CameraScanner({ onScan, onClose }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const timerRef = useRef(null)
  const [cameraError, setCameraError] = useState('')
  useEffect(() => {
    let active = true
    async function start() {
      try {
        if (!navigator.mediaDevices?.getUserMedia) throw new Error('Camera access is unavailable. Use the code entry below.')
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
        if (!active) { stream.getTracks().forEach((track) => track.stop()); return }
        streamRef.current = stream
        videoRef.current.srcObject = stream
        await videoRef.current.play()
        if (!('BarcodeDetector' in window)) throw new Error('This browser cannot read QR codes with its camera. Enter the code manually below.')
        const detector = new window.BarcodeDetector({ formats: ['qr_code'] })
        const scan = async () => {
          if (!active || !videoRef.current) return
          try {
            const codes = await detector.detect(videoRef.current)
            if (codes[0]?.rawValue) { onScan(codes[0].rawValue); return }
          } catch { /* Continue scanning the next camera frame. */ }
          timerRef.current = window.setTimeout(scan, 250)
        }
        scan()
      } catch (error) { setCameraError(error.message || 'Could not open the camera.') }
    }
    start()
    return () => {
      active = false
      window.clearTimeout(timerRef.current)
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [onScan])
  return <div className="member-camera-panel"><video ref={videoRef} muted playsInline /><p><Camera size={16} /> Point the camera at the gym QR code.</p>{cameraError && <p className="attendance-inline-error" role="status">{cameraError}</p>}<button type="button" className="member-text-button" onClick={onClose}>Close camera</button></div>
}

export default function MemberAttendance() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [cameraOpen, setCameraOpen] = useState(false)
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setData(await getMyAttendance()) } catch (requestError) { setError(explainAttendanceError(requestError)) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])
  const handleScan = useCallback(async (payload) => {
    if (!payload || busy) return
    setCameraOpen(false)
    setBusy(true); setMessage(''); setError('')
    try {
      const result = await scanAttendanceQr(payload.trim())
      setMessage(result.action === 'CHECK_IN' ? 'Checked in. Have a good workout!' : 'Checked out. Your visit time has been saved.')
      setCameraOpen(false); await load()
    } catch (requestError) { setError(explainAttendanceError(requestError)) }
    finally { setBusy(false) }
  }, [busy, load])
  const visits = data?.visits ?? []
  return <section className="member-panel member-attendance-panel">
    <div className="member-section-heading"><div><h2>Gym attendance</h2><p>Scan the entrance QR when you arrive and scan again when you leave.</p></div><button className="member-profile-edit-button" type="button" onClick={load}><RefreshCw size={15} /> Refresh</button></div>
    {data && <div className="attendance-member-summary"><article><span><Clock3 size={17} /></span><small>TODAY’S TOTAL</small><strong>{duration(data.totalDurationSeconds)}</strong></article><article><span><ScanLine size={17} /></span><small>VISIT STATUS</small><strong>{data.currentlyInside ? 'Checked in' : 'Checked out'}</strong></article>{data.currentCheckInAt && <article><span><Camera size={17} /></span><small>CHECKED IN AT</small><strong>{new Date(data.currentCheckInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</strong></article>}</div>}
    <div className="attendance-member-actions"><button type="button" onClick={() => setCameraOpen((open) => !open)} disabled={busy}><Camera size={17} /> {cameraOpen ? 'Camera open' : 'Scan gym QR'}</button></div>
    {cameraOpen && <CameraScanner onScan={handleScan} onClose={() => setCameraOpen(false)} />}
    {message && <div className="attendance-success" role="status">{message}</div>}{error && <div className="member-error" role="alert">{error}</div>}
    <div className="attendance-history-heading"><h3>Recent visits</h3><span>Last 30 days</span></div>
    {loading ? <div className="member-loading">Loading your attendance…</div> : visits.length ? <div className="member-table-scroll"><table><thead><tr><th>Date</th><th>Arrived</th><th>Left</th><th>Time inside</th><th>Status</th></tr></thead><tbody>{visits.map((visit) => <tr key={visit.visitId}><td>{new Date(visit.checkInAt).toLocaleDateString()}</td><td>{new Date(visit.checkInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td><td>{visit.checkOutAt ? new Date(visit.checkOutAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</td><td>{duration(visit.durationSeconds)}</td><td>{visit.currentlyInside ? 'Inside now' : 'Complete'}</td></tr>)}</tbody></table></div> : <p className="member-empty">No gym visits yet. Scan the QR at the entrance to start your first visit.</p>}
  </section>
}
