export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'FORBIDDEN_ROLE'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'INVALID_STATE_TRANSITION'
  | 'INSUFFICIENT_STOCK'
  | 'CAPACITY_EXCEEDED'
  | 'TEAM_NOT_AVAILABLE'
  | 'BUSINESS_RULE'
  | 'INTERNAL_ERROR'
  | 'NETWORK_OFFLINE'

export class ApiError extends Error {
  readonly status: number
  readonly code: ApiErrorCode
  readonly details: Record<string, unknown> | undefined

  constructor(status: number, code: ApiErrorCode, message: string, details?: Record<string, unknown>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  static offline(): ApiError {
    return new ApiError(0, 'NETWORK_OFFLINE', 'No connection. Check your network and try again.')
  }

  get isOffline(): boolean {
    return this.code === 'NETWORK_OFFLINE'
  }

  /** Field-level messages from a VALIDATION_ERROR: { fieldName: message }. */
  get fieldErrors(): Record<string, string> {
    const fields = this.details?.fields
    return fields && typeof fields === 'object' ? (fields as Record<string, string>) : {}
  }
}

export function isApiError(e: unknown): e is ApiError {
  return e instanceof ApiError
}
