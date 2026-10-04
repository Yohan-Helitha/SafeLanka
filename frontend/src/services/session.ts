import { STORAGE_KEYS } from '@/constants/app'

let actingUserId: string | null = null

try {
  actingUserId = localStorage.getItem(STORAGE_KEYS.actingUser)
} catch {
  /* storage unavailable */
}

export function getActingUserId(): string | null {
  return actingUserId
}

export function setActingUserId(id: string | null): void {
  actingUserId = id
  try {
    if (id) localStorage.setItem(STORAGE_KEYS.actingUser, id)
    else localStorage.removeItem(STORAGE_KEYS.actingUser)
  } catch {
    /* storage unavailable */
  }
}
