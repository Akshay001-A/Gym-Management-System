const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const VIEW_LABELS = { week: 'Week', month: 'Month', year: 'Year' }

function dateFromIso(value) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function isoDate(value) {
  return value.toISOString().slice(0, 10)
}

function addDays(value, amount) {
  const next = dateFromIso(value)
  next.setUTCDate(next.getUTCDate() + amount)
  return isoDate(next)
}

export function getAttendancePeriod(view, anchorDate, today) {
  const anchor = dateFromIso(anchorDate)
  let start
  let end
  if (view === 'week') {
    start = new Date(anchor)
    const mondayOffset = (start.getUTCDay() + 6) % 7
    start.setUTCDate(start.getUTCDate() - mondayOffset)
    end = new Date(start)
    end.setUTCDate(end.getUTCDate() + 6)
  } else if (view === 'month') {
    start = new Date(Date.UTC(anchor.getUTCFullYear(), anchor.getUTCMonth(), 1))
    end = new Date(Date.UTC(anchor.getUTCFullYear(), anchor.getUTCMonth() + 1, 0))
  } else {
    start = new Date(Date.UTC(anchor.getUTCFullYear(), 0, 1))
    end = new Date(Date.UTC(anchor.getUTCFullYear(), 11, 31))
  }
  const periodStart = isoDate(start)
  const periodEnd = isoDate(end)
  return { from: periodStart, to: periodEnd > today ? today : periodEnd, periodStart, periodEnd }
}

export function AttendanceCalendar({
  view, onViewChange, anchorDate, onNavigate, period, selectedDate, onDateSelect,
  countsByDate, loading, memberView = false,
}) {
  const countFor = (day) => Number(countsByDate[day] || 0)
  const monthLabel = new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(dateFromIso(anchorDate))
  const year = dateFromIso(anchorDate).getUTCFullYear()
  const today = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(new Date())
  const dayCount = view === 'week' ? 7 : new Date(Date.UTC(year, dateFromIso(anchorDate).getUTCMonth() + 1, 0)).getUTCDate()
  const listStart = view === 'week' ? period.periodStart : view === 'month'
    ? `${year}-${String(dateFromIso(anchorDate).getUTCMonth() + 1).padStart(2, '0')}-01`
    : `${year}-01-01`
  const initialOffset = view === 'week' ? 0 : view === 'month'
    ? (dateFromIso(listStart).getUTCDay() + 6) % 7
    : (dateFromIso(listStart).getUTCDay() + 6) % 7
  const daysToRender = view === 'week' ? 7 : view === 'month' ? dayCount : 0
  const gridDays = Array.from({ length: initialOffset }, () => null)
  for (let index = 0; index < daysToRender; index += 1) gridDays.push(addDays(listStart, index))

  let yearCells = []
  let yearWeeks = 0
  let monthStarts = []
  if (view === 'year') {
    const firstDay = `${year}-01-01`
    const firstOffset = (dateFromIso(firstDay).getUTCDay() + 6) % 7
    const totalDays = (Date.UTC(year + 1, 0, 1) - Date.UTC(year, 0, 1)) / 86400000
    yearWeeks = Math.ceil((firstOffset + totalDays) / 7)
    yearCells = Array.from({ length: firstOffset }, () => ({ date: null, column: null, row: null }))
    for (let index = 0; index < totalDays; index += 1) {
      const date = addDays(firstDay, index)
      const offset = firstOffset + index
      yearCells.push({ date, column: Math.floor(offset / 7), row: offset % 7 })
    }
    monthStarts = Array.from({ length: 12 }, (_, month) => {
      const day = `${year}-${String(month + 1).padStart(2, '0')}-01`
      const elapsed = (Date.UTC(year, month, 1) - Date.UTC(year, 0, 1)) / 86400000
      return { label: new Intl.DateTimeFormat(undefined, { month: 'short', timeZone: 'UTC' }).format(dateFromIso(day)), column: Math.floor((firstOffset + elapsed) / 7) }
    })
  }
  const rangeLabel = view === 'year' ? String(year) : view === 'month' ? monthLabel
    : `${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(dateFromIso(period.periodStart))} – ${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' }).format(dateFromIso(period.periodEnd))}`
  const attendedDays = Object.values(countsByDate).filter((count) => Number(count) > 0).length

  return <section className="attendance-calendar-card">
    <div className="attendance-calendar-toolbar">
      <div><p className="eyebrow">ATTENDANCE OVERVIEW</p><h2>{memberView ? 'Your attendance' : 'Gym attendance calendar'}</h2><p>{memberView ? 'Green days show when you visited the gym.' : 'Select a day to see every member’s present or absent status.'}</p></div>
      <div className="attendance-calendar-controls" aria-label="Attendance calendar controls">
        <div className="attendance-view-switch" role="group" aria-label="Calendar period">
          {Object.entries(VIEW_LABELS).map(([key, label]) => <button type="button" key={key} className={view === key ? 'active' : ''} aria-pressed={view === key} onClick={() => onViewChange(key)}>{label}</button>)}
        </div>
        <div className="attendance-period-nav"><button type="button" aria-label={`Previous ${view}`} onClick={() => onNavigate(-1)}>‹</button><strong>{rangeLabel}</strong><button type="button" aria-label={`Next ${view}`} disabled={period.periodEnd >= today} onClick={() => onNavigate(1)}>›</button></div>
      </div>
    </div>
    {loading ? <div className="member-loading">Loading attendance calendar…</div> : view === 'year' ? <div className="attendance-year-scroll"><div className="attendance-year-grid" style={{ '--weeks': yearWeeks }}>
      <span className="attendance-weekday-label attendance-year-corner" />
      {monthStarts.map((month) => <span key={month.label} className="attendance-month-label" style={{ gridColumn: month.column + 2, gridRow: 1 }}>{month.label}</span>)}
      {['Mon', 'Wed', 'Fri'].map((day, index) => <span key={day} className="attendance-weekday-label" style={{ gridColumn: 1, gridRow: index * 2 + 2 }}>{day}</span>)}
      {yearCells.map((cell, index) => {
        if (!cell.date) return <span key={`pad-${index}`} className="attendance-day-pad" style={{ gridColumn: 2 + Math.floor(index / 7), gridRow: 2 + (index % 7) }} />
        const count = countFor(cell.date)
        const future = cell.date > today
        const level = count ? memberView ? 4 : Math.min(4, Math.ceil(count / Math.max(1, ...Object.values(countsByDate).map(Number)) * 4)) : 0
        return <button type="button" key={cell.date} title={`${cell.date}: ${memberView ? count ? 'Present' : future ? 'Future date' : 'Absent' : `${count} present`}`} aria-label={`${cell.date}: ${memberView ? count ? 'present' : future ? 'future date' : 'absent' : `${count} members present`}`} aria-pressed={selectedDate === cell.date} disabled={future} className={`attendance-day-cell level-${level}${selectedDate === cell.date ? ' selected' : ''}${future ? ' future' : ''}`} style={{ gridColumn: 2 + cell.column, gridRow: 2 + cell.row }} onClick={() => onDateSelect(cell.date)} />
      })}
    </div></div> : <div className="attendance-month-grid">
      {WEEKDAYS.map((day) => <span key={day} className="attendance-month-weekday">{day}</span>)}
      {gridDays.map((day, index) => {
        if (!day) return <span key={`pad-${index}`} className="attendance-month-pad" />
        const count = countFor(day)
        const future = day > today
        return <button key={day} type="button" disabled={future} aria-pressed={selectedDate === day} title={`${day}: ${memberView ? count ? 'Present' : future ? 'Future date' : 'Absent' : `${count} present`}`} className={`attendance-month-day${count ? ' present' : ''}${selectedDate === day ? ' selected' : ''}${future ? ' future' : ''}`} onClick={() => onDateSelect(day)}><span>{Number(day.slice(-2))}</span><small>{future ? '—' : memberView ? count ? 'Present' : 'Absent' : count ? `${count} present` : 'No visits'}</small></button>
      })}
    </div>}
    <div className="attendance-calendar-footer"><span>{memberView ? `${attendedDays} days attended` : `${attendedDays} days with visits in this period`}</span><div className="attendance-calendar-legend"><span>Less</span><i className="level-0" /><i className="level-1" /><i className="level-2" /><i className="level-3" /><i className="level-4" /><span>More</span></div></div>
  </section>
}
