import { lazy, Suspense } from 'react'
import type { MapMarker } from './MapView'

const MapView = lazy(() => import('./MapView'))

interface Props {
  markers: MapMarker[]
  onPick?: (latitude: number, longitude: number) => void
  height?: number
  zoom?: number
  /** Describes the map for screen readers. */
  label: string
}

/** Loads Leaflet only when a map is on screen. */
export function LocationMap(props: Props) {
  return (
    <Suspense fallback={<div style={{ height: props.height ?? 240 }} className="grid w-full place-items-center rounded-control border border-line text-sm text-muted">Loading map</div>}>
      <MapView {...props} />
    </Suspense>
  )
}

export type { MapMarker }
