export const APP_NAME = 'SafeLanka'
export const EMERGENCY_NUMBER = '117'

export const STORAGE_KEYS = {
  actingUser: 'safelanka.actingUser',
} as const

/** Polling intervals in milliseconds. */
export const POLL = {
  alerts: 20_000,
  assignment: 15_000,
  situation: 30_000,
  queue: 30_000,
  outbox: 8_000,
} as const

export const LIMITS = {
  description: { min: 10, max: 500 },
  warningTitle: { min: 5, max: 80 },
  warningMessage: { min: 10, max: 1000 },
  sms: { min: 10, max: 160 },
  instructions: { min: 5, max: 300 },
  comment: { min: 5, max: 300 },
  photoBytes: 5 * 1024 * 1024,
} as const

export const MOCK_LATENCY_MS = 280
