import { apiRequest, saveSession } from './client'

export async function login(email, password) {
  const session = await apiRequest('/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  return saveSession(session)
}

export async function registerMember(account, profile) {
  const session = await apiRequest('/auth/register', {
    method: 'POST',
    body: { account, profile },
  })
  return saveSession(session)
}
