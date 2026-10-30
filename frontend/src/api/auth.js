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
