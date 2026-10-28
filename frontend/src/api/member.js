import { apiRequest } from './client'

export function getMyProfile(options = {}) { return apiRequest('/members/me', options) }
export function updateMyProfile(profile) { return apiRequest('/members/me', { method: 'PATCH', body: profile }) }
export function getMyMemberships(options = {}) { return apiRequest('/memberships/me', options) }
export function getMembershipPlans(options = {}) { return apiRequest('/memberships/plans', options) }
export function enrollInPlan(plan) { return apiRequest('/memberships/me', { method: 'POST', body: { plan } }) }
export function renewPlan(plan) { return apiRequest('/memberships/me/renewals', { method: 'POST', body: { plan } }) }
export function cancelMyMembership(id) { return apiRequest('/memberships/me/' + encodeURIComponent(id) + '/cancel', { method: 'POST' }) }
export function getMyPayments(options = {}) { return apiRequest('/memberships/me/payments?page=0&size=20', options) }
export function startSimulatedPayment(id, key) {
  return apiRequest('/memberships/me/' + encodeURIComponent(id) + '/payments', {
    method: 'POST',
    headers: { 'Idempotency-Key': key },
  })
}
export function simulatePaymentOutcome(id, outcome) {
  return apiRequest('/memberships/me/payments/' + encodeURIComponent(id) + '/simulate', {
    method: 'POST',
    body: { outcome },
  })
}
export function getMemberTrainers(options = {}) { return apiRequest('/trainers', options) }
