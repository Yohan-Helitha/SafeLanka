import { useCallback, useState } from 'react'
import type { LatLng } from '@/types'
import { COLOMBO_FIX, insideSriLanka } from '@/utils/geo'

export interface GeoState {
  status: 'idle' | 'locating' | 'ok' | 'unavailable'
  fix: LatLng | null
  accuracyMetres: number | null
  reason: string | null
}

const IDLE: GeoState = { status: 'idle', fix: null, accuracyMetres: null, reason: null }

export function useGeolocation() {
  const [state, setState] = useState<GeoState>(IDLE)
  /** Demo switch for the "GPS is off" alternative flow. */
  const [simulateFailure, setSimulateFailure] = useState(false)

  const locate = useCallback(() => {
    if (simulateFailure) {
      setState({ status: 'unavailable', fix: null, accuracyMetres: null, reason: 'GPS is switched off on this phone.' })
      return
    }
    if (!('geolocation' in navigator)) {
      setState({ status: 'unavailable', fix: null, accuracyMetres: null, reason: 'This device cannot share its location.' })
      return
    }
    setState({ ...IDLE, status: 'locating' })
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const raw = { latitude: pos.coords.latitude, longitude: pos.coords.longitude }
        // Outside Sri Lanka (for example a laptop abroad): fall back to a Colombo fix so the demo works.
        const inside = insideSriLanka(raw)
        setState({
          status: 'ok',
          fix: inside ? raw : COLOMBO_FIX,
          accuracyMetres: inside ? Math.round(pos.coords.accuracy) : 25,
          reason: null,
        })
      },
      (err) =>
        setState({
          status: 'unavailable',
          fix: null,
          accuracyMetres: null,
          reason: err.code === err.PERMISSION_DENIED ? 'Location permission was not given.' : 'We could not get a GPS fix.',
        }),
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }, [simulateFailure])

  const reset = useCallback(() => setState(IDLE), [])

  return { ...state, locate, reset, simulateFailure, setSimulateFailure }
}
