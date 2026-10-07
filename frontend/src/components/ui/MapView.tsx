import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useRef } from 'react'

export interface MapMarker {
  id: string
  latitude: number
  longitude: number
  label?: string
  tone?: 'signal' | 'ok' | 'caution' | 'danger'
}

interface Props {
  markers: MapMarker[]
  /** Called with the clicked position; makes the map a location picker. */
  onPick?: (latitude: number, longitude: number) => void
  height?: number | string
  zoom?: number
  label: string
  className?: string
  darkTheme?: boolean
  selectedId?: string
  onMarkerClick?: (marker: MapMarker) => void
}

const COLOUR = { signal: '#22D3EE', ok: '#34D399', caution: '#FBBF24', danger: '#F87171' }
const COLOMBO: L.LatLngTuple = [6.9271, 79.8612]

/** Leaflet map on OpenStreetMap tiles: no API key. Markers are drawn as circles, so no image assets are needed. */
export default function MapView({
  markers,
  onPick,
  height = 240,
  zoom = 14,
  label,
  className,
  darkTheme = false,
  selectedId,
  onMarkerClick,
}: Props) {
  const el = useRef<HTMLDivElement>(null)
  const map = useRef<L.Map | null>(null)
  const layer = useRef<L.LayerGroup | null>(null)
  const pick = useRef(onPick)
  pick.current = onPick
  // Redraw only when marker content changes, so typing in a form beside the map never resets the view.
  const markerKey = JSON.stringify(markers)

  useEffect(() => {
    if (!el.current) return
    const m = L.map(el.current, { scrollWheelZoom: false }).setView(COLOMBO, zoom)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(m)
    layer.current = L.layerGroup().addTo(m)
    m.on('click', (e) => pick.current?.(e.latlng.lat, e.latlng.lng))
    map.current = m

    const resizeObserver = new ResizeObserver(() => {
      m.invalidateSize()
    })
    resizeObserver.observe(el.current)

    // Trigger size calculation after DOM settle
    setTimeout(() => {
      m.invalidateSize()
    }, 150)

    return () => {
      resizeObserver.disconnect()
      m.remove()
      map.current = null
      layer.current = null
    }
  }, [zoom])

  useEffect(() => {
    const m = map.current
    const g = layer.current
    if (!m || !g) return
    g.clearLayers()
    markers.forEach((p) => {
      const colour = COLOUR[p.tone ?? 'signal']
      const isSelected = selectedId === p.id
      const dot = L.circleMarker([p.latitude, p.longitude], {
        radius: isSelected ? 14 : 11,
        color: isSelected ? '#FFFFFF' : '#0B1324',
        weight: isSelected ? 4 : 3,
        fillColor: colour,
        fillOpacity: 1,
        className: 'map-marker-glow',
      })
      if (p.label) dot.bindTooltip(p.label, { direction: 'top', offset: [0, -10], className: 'text-foreground font-semibold' })
      if (onMarkerClick) {
        dot.on('click', (e) => {
          L.DomEvent.stopPropagation(e)
          onMarkerClick(p)
        })
      }
      dot.addTo(g)
    })

    const selected = markers.find((item) => item.id === selectedId)
    if (selected) {
      m.setView([selected.latitude, selected.longitude], Math.max(m.getZoom(), 14))
    } else if (markers.length === 1) {
      m.setView([markers[0].latitude, markers[0].longitude], zoom)
    } else if (markers.length > 1) {
      m.fitBounds(
        L.latLngBounds(markers.map((p) => [p.latitude, p.longitude] as L.LatLngTuple)),
        { padding: [28, 28], maxZoom: 15 }
      )
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [markerKey, zoom, selectedId])

  const containerHeight = typeof height === 'number' ? `${height}px` : height
  const containerClass = className ?? 'z-0 w-full overflow-hidden rounded-control border border-line'

  return (
    <div
      ref={el}
      role="application"
      aria-label={label}
      style={{ height: containerHeight }}
      className={`${darkTheme ? 'tactical-dark-map ' : ''}${containerClass}`}
    />
  )
}
