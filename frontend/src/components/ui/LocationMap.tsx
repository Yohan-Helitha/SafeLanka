import { lazy, Suspense } from 'react'
import type { MapMarker } from './MapView'

const MapView = lazy(() => import('./MapView'))

interface Props {
  markers: MapMarker[]
  onPick?: (latitude: number, longitude: number) => void
  height?: number | string
  zoom?: number
  /** Describes the map for screen readers. */
  label: string
  className?: string
  darkTheme?: boolean
  selectedId?: string
  onMarkerClick?: (marker: MapMarker) => void
}

/** Loads Leaflet only when a map is on screen. */
export function LocationMap(props: Props) {
  const heightStyle = typeof props.height === 'number' ? `${props.height}px` : (props.height ?? 240)
  return (
    <Suspense
      fallback={
        <div style={{ height: heightStyle }} className="grid w-full place-items-center rounded-control border border-line text-sm text-muted">
          Loading map...
        </div>
      }
    >
      <MapView {...props} />
    </Suspense>
  )
}

export type { MapMarker }
