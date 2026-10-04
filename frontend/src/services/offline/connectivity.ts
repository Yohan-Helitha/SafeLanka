type Listener = () => void

let forcedOffline = false
const listeners = new Set<Listener>()

function emit() {
  listeners.forEach((l) => l())
}

if (typeof window !== 'undefined') {
  window.addEventListener('online', emit)
  window.addEventListener('offline', emit)
}

export const connectivity = {
  /** Offline when the demo switch is on or the browser reports no network. */
  isOffline(): boolean {
    return forcedOffline || (typeof navigator !== 'undefined' && !navigator.onLine)
  },
  isForced(): boolean {
    return forcedOffline
  },
  setForcedOffline(value: boolean): void {
    forcedOffline = value
    emit()
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
