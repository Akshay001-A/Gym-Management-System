const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, { status = 0, details = null, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

function getErrorMessage(payload, fallback) {
  if (typeof payload === 'string' && payload.trim()) return payload
  if (!payload || typeof payload !== 'object') return fallback

  const directMessage = payload.error || payload.message || payload.detail
  if (typeof directMessage === 'string' && directMessage.trim()) return directMessage

  const fieldErrors = Object.entries(payload)
    .filter(([, value]) => typeof value === 'string')
    .map(([field, message]) => `${field}: ${message}`)

  return fieldErrors.length ? fieldErrors.join('; ') : fallback
}

export async function apiRequest(path, { method = 'GET', body, signal, headers = {} } = {}) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = `${API_BASE_URL}${normalizedPath}`
  const requestHeaders = new Headers(headers)
  requestHeaders.set('Accept', 'application/json')

  const options = { method, headers: requestHeaders, signal }
  if (body !== undefined) {
    requestHeaders.set('Content-Type', 'application/json')
    options.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(url, options)
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError('Could not reach the API Gateway. Check that it is running and try again.', { cause: error })
  }

  const responseText = await response.text()
  let payload = null
  if (responseText) {
    try {
      payload = JSON.parse(responseText)
    } catch (error) {
      throw new ApiError(
        response.ok ? 'The server returned an invalid response.' : `Request failed with status ${response.status}.`,
        { status: response.status, details: responseText, cause: error },
      )
    }
  }

  if (!response.ok) {
    throw new ApiError(
      getErrorMessage(payload, `Request failed with status ${response.status}.`),
      { status: response.status, details: payload },
    )
  }

  return payload
}
