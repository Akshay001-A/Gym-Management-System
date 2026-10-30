import { apiRequest, saveSession } from './client'

export async function login(email, password) {
  const session = await apiRequest('/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  return saveSession(session)
}

export function requestMemberRegistrationCode(identifier) {
  return apiRequest('/auth/register/request-code', {
    method: 'POST',
    body: { identifier },
  })
}

export async function completeMemberRegistration(identifier, code, password) {
  const session = await apiRequest('/auth/register/complete', {
    method: 'POST',
    body: { identifier, code, password },
  })
  return saveSession(session)
}

export function requestPasswordResetCode(email) {
  return apiRequest('/auth/password/request-code', { method: 'POST', body: { email } })
}

export function completePasswordReset(email, code, password) {
  return apiRequest('/auth/password/reset', { method: 'POST', body: { email, code, password } })
}

export function requestMemberEmailChangeCode(newEmail) {
  return apiRequest('/auth/email/change/request-code', { method: 'POST', body: { newEmail } })
}

export async function completeMemberEmailChange(newEmail, code) {
  const session = await apiRequest('/auth/email/change/complete', {
    method: 'POST',
    body: { newEmail, code },
  })
  return saveSession(session)
}
