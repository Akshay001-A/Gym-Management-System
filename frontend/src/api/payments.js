import { apiRequest, ApiError } from './client'

export function createPaymentOrder(planName, idempotencyKey) {
  return apiRequest('/payments/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: { planName },
  })
}

export function verifyPayment({ razorpayOrderId, razorpayPaymentId, razorpaySignature }) {
  return apiRequest('/payments/verify', {
    method: 'POST',
    body: { razorpayOrderId, razorpayPaymentId, razorpaySignature },
  })
}

export async function getPaymentHistory({ page = 0, size = 10, signal } = {}) {
  const result = await apiRequest(`/payments/history?page=${page}&size=${size}`, { signal })
  if (!result || !Array.isArray(result.content)) {
    throw new ApiError('The Payment Service returned an invalid payment history.')
  }
  return result
}

export function getPaymentDetails(paymentId, { signal } = {}) {
  return apiRequest(`/payments/${encodeURIComponent(paymentId)}`, { signal })
}

export function getAdminRevenueReport({ period = 'MONTHLY', year, month, page = 0, size = 25, signal }) {
  const params = new URLSearchParams({ period, year: String(year), page: String(page), size: String(size) })
  if (period === 'MONTHLY') params.set('month', String(month))
  return apiRequest(`/payments/admin/revenue/report?${params}`, { signal })
}

export function createIdempotencyKey() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
