import { apiRequest, ApiError } from './client'

function todayAsDate() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

export async function getOverviewCounts({ signal } = {}) {
  const [memberships, trainers, revenue] = await Promise.allSettled([
    apiRequest('/memberships', { signal }),
    apiRequest('/trainers', { signal }),
    apiRequest('/payments/admin/revenue/monthly', { signal }),
  ])

  let activeMemberships = null
  let trainerCount = null

  if (memberships.status === 'fulfilled') {
    if (!Array.isArray(memberships.value)) throw new ApiError('The Membership Service returned an invalid membership list.')
    const today = todayAsDate()
    activeMemberships = memberships.value.filter((membership) =>
      membership.status?.toUpperCase() === 'ACTIVE'
      && typeof membership.startDate === 'string'
      && typeof membership.endDate === 'string'
      && membership.startDate <= today
      && membership.endDate >= today,
    ).length
  }

  if (trainers.status === 'fulfilled') {
    if (!Array.isArray(trainers.value)) throw new ApiError('The Trainer Service returned an invalid trainer list.')
    trainerCount = trainers.value.length
  }

  return {
    activeMemberships,
    trainerCount,
    membershipUnavailable: memberships.status === 'rejected',
    trainerUnavailable: trainers.status === 'rejected',
    monthlyRevenue: revenue.status === 'fulfilled' ? revenue.value : null,
    revenueUnavailable: revenue.status === 'rejected',
  }
}
