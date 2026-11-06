import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Clock3, Mail, QrCode, RefreshCw, Search, Send } from 'lucide-react'
import { createAttendanceQrToken, getAdminAttendance, getAdminAttendanceCalendar, getAttendanceReminderSettings, sendAttendanceReminderNow, updateAttendanceReminder } from '../api/attendance'
import { AttendanceCalendar, getAttendancePeriod } from './AttendanceCalendar'
import '../styles/attendance-calendar.css'

const PAGE_SIZE = 100
const today = () => new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date())

function shiftDate(value, view, direction, maximum) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  if (view === 'week') date.setUTCDate(date.getUTCDate() + direction * 7)
  else if (view === 'year') date.setUTCFullYear(date.getUTCFullYear() + direction)
  else {
    const targetMonth = date.getUTCMonth() + direction
    const target = new Date(Date.UTC(date.getUTCFullYear(), targetMonth, 1))
    target.setUTCDate(Math.min(day, new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate()))
    return target.toISOString().slice(0, 10) > maximum ? maximum : target.toISOString().slice(0, 10)
  }
  return date.toISOString().slice(0, 10) > maximum ? maximum : date.toISOString().slice(0, 10)
}

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
  const [calendarView, setCalendarView] = useState('year')
  const [calendarData, setCalendarData] = useState(null)
  const [calendarLoading, setCalendarLoading] = useState(true)
  const [calendarError, setCalendarError] = useState('')
  const [rosterFilter, setRosterFilter] = useState('all')
  const [rosterSearch, setRosterSearch] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [attendanceLoadedAt, setAttendanceLoadedAt] = useState(Date.now())
  const [expandedMember, setExpandedMember] = useState(null)
  const [reminderMembers, setReminderMembers] = useState([])
  const [emailConfigured, setEmailConfigured] = useState(false)
  const [reminderLoading, setReminderLoading] = useState(true)
  const [reminderError, setReminderError] = useState('')
  const [reminderSearch, setReminderSearch] = useState('')
  const [savingReminderId, setSavingReminderId] = useState(null)
  const [sendingReminderId, setSendingReminderId] = useState(null)
  const [manualReminderFeedback, setManualReminderFeedback] = useState(null)
  const attendancePeriod = getAttendancePeriod(calendarView, date, today())

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

  useEffect(() => {
    const controller = new AbortController()
    setCalendarLoading(true)
    setCalendarError('')
    getAdminAttendanceCalendar({
      from: attendancePeriod.from, to: attendancePeriod.to, signal: controller.signal,
    }).then((value) => setCalendarData(value))
      .catch((requestError) => { if (requestError.name !== 'AbortError') setCalendarError(requestError.message) })
      .finally(() => { if (!controller.signal.aborted) setCalendarLoading(false) })
    return () => controller.abort()
  }, [attendancePeriod.from, attendancePeriod.to, reload])

  useEffect(() => {
    const controller = new AbortController()
    getAttendanceReminderSettings({ signal: controller.signal })
      .then((settings) => {
        setReminderMembers(settings.members ?? [])
        setEmailConfigured(Boolean(settings.emailConfigured))
        setReminderError('')
      })
      .catch((requestError) => { if (requestError.name !== 'AbortError') setReminderError(requestError.message) })
      .finally(() => { if (!controller.signal.aborted) setReminderLoading(false) })
    return () => controller.abort()
  }, [])

  async function changeReminder(member, enabled) {
    if (savingReminderId != null) return
    setSavingReminderId(member.memberId)
    setReminderError('')
    try {
      const updated = await updateAttendanceReminder(member.memberId, enabled)
      setReminderMembers((current) => current.map((item) => item.memberId === updated.memberId ? updated : item))
    } catch (requestError) { setReminderError(requestError.message) }
    finally { setSavingReminderId(null) }
  }

  async function sendImmediateReminder(member) {
    if (sendingReminderId != null) return
    setSendingReminderId(member.memberId)
    setManualReminderFeedback(null)
    try {
      await sendAttendanceReminderNow(member.memberId)
      setManualReminderFeedback({ memberId: member.memberId, tone: 'success', message: `Email sent to ${member.name}.` })
    } catch (requestError) {
      setManualReminderFeedback({ memberId: member.memberId, tone: 'error', message: requestError.message })
    } finally { setSendingReminderId(null) }
  }

  const rows = result?.content ?? []
  const filteredReminderMembers = reminderMembers.filter((member) =>
    [member.name, member.email, member.memberId].some((value) => String(value ?? '').toLowerCase().includes(reminderSearch.trim().toLowerCase())),
  )
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
  const calendarCounts = useMemo(() => {
    const counts = {}
    calendarData?.members?.forEach((member) => member.presentDates?.forEach((day) => { counts[day] = (counts[day] ?? 0) + 1 }))
    return counts
  }, [calendarData])
  const allAttendanceRows = useMemo(() => {
    const totalsById = new Map(memberTotals.map((member) => [member.memberId, member]))
    return (calendarData?.members ?? []).map((member) => {
      const summary = totalsById.get(member.memberId)
      const present = member.presentDates?.includes(date) ?? false
      return { ...member, present, totalSeconds: summary?.totalSeconds ?? 0, visitCount: summary?.visitCount ?? 0, visits: summary?.visits ?? [] }
    })
  }, [calendarData, date, memberTotals])
  const attendanceRows = useMemo(() => allAttendanceRows.filter((member) =>
    (rosterFilter === 'all' || (rosterFilter === 'present' ? member.present : !member.present))
      && [member.name, member.memberId].some((value) => String(value ?? '').toLowerCase().includes(rosterSearch.trim().toLowerCase())),
  ), [allAttendanceRows, rosterFilter, rosterSearch])
  const presentCount = allAttendanceRows.filter((member) => member.present).length
  const totalMemberCount = allAttendanceRows.length
  return <div className="page-content attendance-page">
    <div className="page-title-row"><div><p className="eyebrow">CLUB OPERATIONS</p><h1>Attendance</h1><p className="page-subtitle">Members scan this code at the entrance to check in and check out.</p></div></div>
    <div className="attendance-admin-grid">
      <section className="surface attendance-qr-card"><div className="attendance-heading"><span><QrCode size={19} /></span><div><h2>Gym entrance QR</h2><p>Refreshes automatically before it expires.</p></div></div>
        {qr?.qrImageDataUrl ? <img className="attendance-qr-image" src={qr.qrImageDataUrl} alt="Current gym attendance QR code" /> : <div className="attendance-qr-placeholder"><QrCode size={44} />{qrError || 'Creating secure QR code…'}</div>}
        <div className="attendance-qr-footer"><span><Clock3 size={15} /> {secondsLeft ? `Refreshes in ${secondsLeft}s` : 'Preparing next code'}</span><button className="button button-secondary" type="button" onClick={refreshQr}><RefreshCw size={15} /> Refresh</button></div>
        {qrError && qr?.qrImageDataUrl && <p className="attendance-inline-error" role="alert">Could not refresh: {qrError}</p>}
      </section>
      <section className="surface attendance-list-card"><div className="directory-toolbar"><div><h2>Attendance by member</h2><p>{calendarData ? `${presentCount} present · ${Math.max(0, totalMemberCount - presentCount)} absent on ${new Date(`${date}T00:00:00`).toLocaleDateString()} · immediate reminders are separate from the 11 p.m. automatic email` : 'Loading daily attendance…'}</p></div><label className="attendance-date">Date<input type="date" value={date} max={today()} onChange={(event) => setDate(event.target.value)} /></label></div>
        <div className="attendance-roster-summary"><strong>{calendarData ? presentCount : '—'} Present</strong><strong className="absent">{calendarData ? Math.max(0, totalMemberCount - presentCount) : '—'} Absent</strong><div className="attendance-roster-filters" role="group" aria-label="Filter attendance list">{[['all', 'All'], ['present', 'Present'], ['absent', 'Absent']].map(([key, label]) => <button type="button" key={key} className={rosterFilter === key ? 'active' : ''} aria-pressed={rosterFilter === key} onClick={() => setRosterFilter(key)}>{label}</button>)}</div><label className="search-box"><Search size={15} /><input aria-label="Search attendance members" value={rosterSearch} onChange={(event) => setRosterSearch(event.target.value)} placeholder="Search members…" /></label></div>
        {manualReminderFeedback && <div className={`attendance-manual-feedback ${manualReminderFeedback.tone}`} role="status">{manualReminderFeedback.message}</div>}
        {calendarError || error ? <div className="member-error" role="alert">{calendarError || error}<button className="button button-secondary" onClick={() => { setError(''); setReload((value) => value + 1) }}>Retry</button></div> : loading || calendarLoading ? <div className="loading-state">Loading member attendance…</div> : attendanceRows.length ? <div className="member-table-scroll"><table className="attendance-roster-table"><thead><tr><th>Member</th><th>Attendance</th><th>Visits</th><th>Total time</th><th>Daily usage</th><th>Visits / reminder</th></tr></thead><tbody>{attendanceRows.map((member) => {
          const status = limitStatus(member.totalSeconds)
          const usage = Math.min(100, member.totalSeconds / 9000 * 100)
          const isExpanded = expandedMember === member.memberId
          const emailAvailable = Boolean(reminderMembers.find((entry) => entry.memberId === member.memberId)?.email)
          return <Fragment key={member.memberId}><tr><td><strong className="attendance-member-name">{member.name}</strong></td><td><span className={`attendance-status ${member.present ? 'present' : 'absent'}`}>{member.present ? 'Present' : 'Absent'}</span></td><td>{member.visitCount || '—'}</td><td><strong>{member.present ? duration(member.totalSeconds) : '—'}</strong></td><td>{member.present ? <div className="attendance-limit-cell"><div className="attendance-limit-track"><span className={status.tone} style={{ width: `${usage}%` }} /><i /></div><small>{duration(member.totalSeconds)} / 2h 30m</small></div> : '—'}</td><td>{member.present ? <button className="attendance-detail-button" type="button" onClick={() => setExpandedMember(isExpanded ? null : member.memberId)}>{isExpanded ? 'Hide visits' : 'View visits'}</button> : date === today() && <button className="attendance-send-reminder" type="button" aria-label={`Send attendance reminder to ${member.name}`} disabled={sendingReminderId != null || !emailConfigured || !emailAvailable} title={!emailAvailable ? 'This member has no email address' : !emailConfigured ? 'Email settings are not available' : ''} onClick={() => sendImmediateReminder(member)}>{sendingReminderId === member.memberId ? <RefreshCw size={13} className="attendance-send-spinner" /> : <Send size={13} />}{sendingReminderId === member.memberId ? 'Sending…' : manualReminderFeedback?.memberId === member.memberId && manualReminderFeedback.tone === 'success' ? 'Send again' : 'Send reminder'}</button>}</td></tr>
            {isExpanded && <tr className="attendance-expanded-row"><td colSpan="6"><div className="attendance-expanded-content"><h3>Visits for {member.name}</h3><div className="member-table-scroll"><table><thead><tr><th>Arrived</th><th>Left</th><th>Time inside</th><th>Status</th></tr></thead><tbody>{member.visits.map((visit) => <tr key={visit.visitId}><td>{visit.checkInAt ? new Date(visit.checkInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'}</td><td>{visit.checkOutAt ? new Date(visit.checkOutAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'}</td><td>{duration((visit.durationSeconds ?? 0) + (visit.currentlyInside ? Math.max(0, Math.floor((Date.now() - attendanceLoadedAt) / 1000)) : 0))}</td><td>{visit.currentlyInside ? 'Inside now' : 'Complete'}</td></tr>)}</tbody></table></div></div></td></tr>}</Fragment>
        })}</tbody></table></div> : <div className="attendance-empty">{totalMemberCount ? 'No members match this search.' : 'No members are registered yet.'}</div>}
      </section>
      <AttendanceCalendar view={calendarView} onViewChange={setCalendarView} anchorDate={date} onNavigate={(direction) => setDate((current) => shiftDate(current, calendarView, direction, today()))} period={attendancePeriod} selectedDate={date} onDateSelect={setDate} countsByDate={calendarCounts} loading={calendarLoading} />
      <section className="surface attendance-reminder-panel"><div className="attendance-reminder-heading"><span><Mail size={18} /></span><div><h2>Daily attendance reminders</h2><p>Enabled members with no visit today receive a friendly email at 11:00 PM India time, inviting them to come tomorrow.</p></div></div>
        {!emailConfigured && <div className="attendance-mail-warning" role="status">Email sending is not configured yet. Add working SMTP settings to the existing microservices .env file before turning reminders on.</div>}
        {reminderError && <div className="member-error" role="alert">{reminderError}</div>}
        <div className="attendance-reminder-toolbar"><span>{reminderMembers.filter((member) => member.enabled).length} members enabled</span><label className="search-box"><Search size={16} /><input aria-label="Search reminder members" value={reminderSearch} onChange={(event) => setReminderSearch(event.target.value)} placeholder="Search members…" />{reminderSearch && <button type="button" aria-label="Clear search" onClick={() => setReminderSearch('')}>&times;</button>}</label></div>
        {reminderLoading ? <div className="loading-state">Loading member reminder settings…</div> : filteredReminderMembers.length ? <div className="member-table-scroll"><table className="attendance-reminder-table"><thead><tr><th>Member</th><th>Email address</th><th>Last reminder</th><th>Email reminders</th></tr></thead><tbody>{filteredReminderMembers.map((member) => <tr key={member.memberId}><td><strong className="attendance-member-name">{member.name}</strong></td><td>{member.email || <span className="attendance-muted">No email address</span>}</td><td>{member.lastReminderSentFor ? new Date(`${member.lastReminderSentFor}T00:00:00`).toLocaleDateString() : 'Not sent'}</td><td><div className="attendance-toggle-wrap"><button type="button" role="switch" aria-checked={member.enabled} aria-label={`${member.enabled ? 'Turn off' : 'Turn on'} reminders for ${member.name}`} className={`attendance-switch ${member.enabled ? 'on' : ''}`} disabled={savingReminderId != null || (!member.enabled && (!emailConfigured || !member.email))} onClick={() => changeReminder(member, !member.enabled)}><span /></button><span>{savingReminderId === member.memberId ? 'Saving…' : member.enabled ? 'On' : 'Off'}</span></div></td></tr>)}</tbody></table></div> : <div className="attendance-empty">{reminderMembers.length ? 'No members match this search.' : 'No members are registered yet.'}</div>}
        <p className="attendance-reminder-note">You can turn reminders off for a member who is ill, away, or taking a break. While enabled, a reminder may be sent each night they miss that day.</p>
      </section>
    </div>
  </div>
}
