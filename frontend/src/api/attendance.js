import { apiRequest } from './client'

export function createAttendanceQrToken() {
  return apiRequest('/members/attendance/qr-token', { method: 'POST' })
}

export function getAdminAttendance({ date, page = 0, size = 50, signal } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (date) params.set('date', date)
  return apiRequest(`/members/attendance?${params}`, { signal })
}

export function getAdminAttendanceCalendar({ from, to, signal } = {}) {
  const params = new URLSearchParams({ from, to })
  return apiRequest(`/members/attendance/calendar?${params}`, { signal })
}

export function scanAttendanceQr(qrPayload) {
  return apiRequest('/members/me/attendance/scan', { method: 'POST', body: { qrPayload } })
}

export function getMyAttendance({ from, to, signal } = {}) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const query = params.size ? `?${params}` : ''
  return apiRequest(`/members/me/attendance${query}`, { signal })
}

export function getAttendanceReminderSettings({ signal } = {}) {
  return apiRequest('/members/attendance/reminders', { signal })
}

export function updateAttendanceReminder(memberId, enabled) {
  return apiRequest(`/members/attendance/reminders/${encodeURIComponent(memberId)}`, {
    method: 'PUT', body: { enabled },
  })
}
