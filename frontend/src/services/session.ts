import { STORAGE_KEYS } from '@/constants/app'

/**
 * Who the app currently acts as.
 *
 * - The access token lives in memory only (never localStorage), so scripts injected into the page
 *   cannot read it. A reload renews it through the httpOnly refresh cookie.
 * - The acting user id is used by demo mode (X-Acting-User) and by the in-browser mock backend.
 */
let accessToken: string | null = null
let actingUserId: string | null = null

try {
  actingUserId = localStorage.getItem(STORAGE_KEYS.actingUser)
} catch {
  /* storage unavailable */
}

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
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

type Listener = () => void
const expiredListeners = new Set<Listener>()

/** Called when a session could not be renewed, so the UI can send the person to the login screen. */
export function onSessionExpired(listener: Listener): () => void {
  expiredListeners.add(listener)
  return () => expiredListeners.delete(listener)
}

export function notifySessionExpired(): void {
  accessToken = null
  expiredListeners.forEach((l) => l())
}
