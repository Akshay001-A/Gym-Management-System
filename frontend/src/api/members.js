import { apiRequest, ApiError } from './client'

const PAGE_SIZE = 10

function parseMemberPage(payload) {
  if (!payload || !Array.isArray(payload.content) || !Number.isFinite(payload.totalElements)) {
    throw new ApiError('The Member Service returned an invalid member list.')
  }
  return payload
}

export async function getMembersPage({ page = 0, size = PAGE_SIZE, signal } = {}) {
  const params = new URLSearchParams({ page, size, sortBy: 'id', direction: 'desc' })
  return parseMemberPage(await apiRequest(`/members?${params}`, { signal }))
}

export async function searchMembers(query, { signal } = {}) {
  const params = new URLSearchParams({ page: 0, size: 100, sortBy: 'id', direction: 'desc' })
  const firstPage = parseMemberPage(await apiRequest(`/members?${params}`, { signal }))
  const members = [...firstPage.content]

  for (let page = 1; page < firstPage.totalPages; page += 1) {
    params.set('page', page)
    const result = parseMemberPage(await apiRequest(`/members?${params}`, { signal }))
    members.push(...result.content)
  }

  const term = query.trim().toLowerCase()
  return {
    content: members.filter((member) =>
      [member.name, member.email, member.mobile, member.id]
        .some((value) => String(value ?? '').toLowerCase().includes(term)),
    ),
    totalElements: firstPage.totalElements,
  }
}

export async function getAllMembers({ signal } = {}) {
  const members = []
  let page = 0
  let pages = 1
  while (page < pages) {
    const result = await getMembersPage({ page, size: 100, signal })
    members.push(...result.content)
    pages = result.totalPages
    page += 1
  }
  return members
}

export function getMember(id, { signal } = {}) {
  return apiRequest(`/members/${encodeURIComponent(id)}`, { signal })
}

export function createMember(member) {
  return apiRequest('/members', { method: 'POST', body: member })
}

export function updateMember(id, member) {
  return apiRequest(`/members/${encodeURIComponent(id)}`, { method: 'PUT', body: member })
}

export function deleteMember(id) {
  return apiRequest(`/members/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
