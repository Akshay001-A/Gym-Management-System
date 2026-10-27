import { apiRequest, ApiError } from './client'

export async function getMemberships({ signal } = {}) {
  const result = await apiRequest('/memberships', { signal })
  if (!Array.isArray(result)) throw new ApiError('The Membership Service returned an invalid membership list.')
  return result
}

export function getMembership(id, { signal } = {}) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { signal })
}

export function assignMembershipPlan(memberId, plan) {
  const params = new URLSearchParams({ memberId, plan })
  return apiRequest(`/memberships/plan?${params}`, { method: 'POST' })
}

export function updateMembership(id, membership) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { method: 'PUT', body: membership })
}

export function deleteMembership(id) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
