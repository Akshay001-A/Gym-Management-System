import { apiRequest, ApiError } from './client'

export async function getTrainers({ signal } = {}) {
  const result = await apiRequest('/trainers', { signal })
  if (!Array.isArray(result)) throw new ApiError('The Trainer Service returned an invalid trainer list.')
  return result
}

export function getTrainer(id, { signal } = {}) {
  return apiRequest(`/trainers/${encodeURIComponent(id)}`, { signal })
}

export function createTrainer(trainer) {
  return apiRequest('/trainers', { method: 'POST', body: trainer })
}

export function updateTrainer(id, trainer) {
  return apiRequest(`/trainers/${encodeURIComponent(id)}`, { method: 'PUT', body: trainer })
}

export function deleteTrainer(id) {
  return apiRequest(`/trainers/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
