import { apiRequest, ApiError } from './client'

export async function getMemberships({ signal } = {}) {
  const result = await apiRequest('/memberships', { signal })
  if (!Array.isArray(result)) throw new ApiError('The Membership Service returned an invalid membership list.')
  return result
}

export async function getMembershipPlans({ signal } = {}) {
  const result = await apiRequest('/memberships/plans', { signal })
  if (!Array.isArray(result)) throw new ApiError('The Membership Service returned an invalid plan list.')
  return result
}

export function getMembership(id, { signal } = {}) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { signal })
}

export function assignMembershipPlan(memberId, plan) {
  return apiRequest('/memberships/admin-assignment', { method: 'POST', body: { memberId, plan } })
}

export function updateMembership(id, membership) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { method: 'PUT', body: membership })
}

export function deleteMembership(id) {
  return apiRequest(`/memberships/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
