import { MOCK_LATENCY_MS } from '@/constants/app'
import type { Page } from '@/types'
import { ApiError } from '../ApiError'
import type { ApiErrorCode } from '../ApiError'
import { connectivity } from '../offline/connectivity'

/** Runs a mock handler with backend-like latency, offline behaviour and copy-on-return. */
export async function mockCall<T>(handler: () => T): Promise<T> {
  if (connectivity.isOffline()) {
    await sleep(60)
    throw ApiError.offline()
  }
  await sleep(MOCK_LATENCY_MS)
  if (connectivity.isOffline()) throw ApiError.offline()
  const result = handler()
  return cloneResult(result)
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** structuredClone, but Blobs and other non-cloneable values pass through unchanged. */
function cloneResult<T>(value: T): T {
  if (value instanceof Blob) return value
  try {
    return structuredClone(value)
  } catch {
    return value
  }
}

const STATUS: Record<ApiErrorCode, number> = {
  VALIDATION_ERROR: 400,
  UNAUTHENTICATED: 401,
  INVALID_CREDENTIALS: 401,
  FORBIDDEN_ROLE: 403,
  PHONE_NOT_VERIFIED: 403,
  CODE_INVALID: 400,
  CODE_EXPIRED: 410,
  ACCOUNT_LOCKED: 423,
  TOO_MANY_REQUESTS: 429,
  SMS_UNAVAILABLE: 503,
  NOT_FOUND: 404,
  CONFLICT: 409,
  INVALID_STATE_TRANSITION: 409,
  INSUFFICIENT_STOCK: 409,
  CAPACITY_EXCEEDED: 409,
  TEAM_NOT_AVAILABLE: 409,
  BUSINESS_RULE: 422,
  INTERNAL_ERROR: 500,
  NETWORK_OFFLINE: 0,
}

/** Throws the same ApiError the backend would. */
export function fail(code: ApiErrorCode, message: string, details?: Record<string, unknown>): never {
  throw new ApiError(STATUS[code], code, message, details)
}

export function validationFail(fields: Record<string, string>): never {
  fail('VALIDATION_ERROR', 'Some details need fixing.', { fields })
}

export function notFound(what: string): never {
  fail('NOT_FOUND', `${what} was not found.`)
}

export function paginate<T>(items: T[], page = 0, size = 20): Page<T> {
  const start = page * size
  return {
    items: items.slice(start, start + size),
    page,
    size,
    totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / size)),
  }
}
