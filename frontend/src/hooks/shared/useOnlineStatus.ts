import { useSyncExternalStore } from 'react'
import { connectivity } from '@/services/offline/connectivity'

/** True when the app can reach the network (and the demo "Offline" switch is off). */
export function useOnlineStatus(): boolean {
  return useSyncExternalStore(
    connectivity.subscribe,
    () => !connectivity.isOffline(),
    () => true,
  )
}
