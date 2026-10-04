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
  height?: number
  zoom?: number
  label: string
}

const COLOUR = { signal: '#22D3EE', ok: '#34D399', caution: '#FBBF24', danger: '#F87171' }
const COLOMBO: L.LatLngTuple = [6.9271, 79.8612]

/** Leaflet map on OpenStreetMap tiles: no API key. Markers are drawn as circles, so no image assets are needed. */
export default function MapView({ markers, onPick, height = 240, zoom = 14, label }: Props) {
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
    return () => {
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
      const dot = L.circleMarker([p.latitude, p.longitude], { radius: 9, color: '#0B1324', weight: 2, fillColor: colour, fillOpacity: 0.95 })
      if (p.label) dot.bindTooltip(p.label)
      dot.addTo(g)
    })
    if (markers.length === 1) m.setView([markers[0].latitude, markers[0].longitude], zoom)
    else if (markers.length > 1) m.fitBounds(L.latLngBounds(markers.map((p) => [p.latitude, p.longitude] as L.LatLngTuple)), { padding: [24, 24], maxZoom: 15 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [markerKey, zoom])

  return <div ref={el} role="application" aria-label={label} style={{ height }} className="z-0 w-full overflow-hidden rounded-control border border-line" />
}
