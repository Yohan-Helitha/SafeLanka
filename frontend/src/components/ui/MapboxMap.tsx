import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
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
  height?: number | string
  label: string
  darkTheme?: boolean
  selectedId?: string
  onMarkerClick?: (marker: MapMarker) => void
  onPick?: (latitude: number, longitude: number) => void
  center?: [number, number] // [lng, lat]
  zoom?: number
  className?: string
}

const COLOUR = { signal: '#22D3EE', ok: '#34D399', caution: '#FBBF24', danger: '#F87171' }

function isSameMarkers(a: MapMarker[], b: MapMarker[]) {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i].id !== b[i].id || a[i].latitude !== b[i].latitude || a[i].longitude !== b[i].longitude) return false
  }
  return true
}

export function MapboxMap({
  markers,
  height = 240,
  label,
  darkTheme = false,
  selectedId,
  onMarkerClick,
  onPick,
  center,
  zoom = 13,
  className,
}: Props) {
  const el = useRef<HTMLDivElement>(null)
  const map = useRef<mapboxgl.Map | null>(null)
  const markersRef = useRef<mapboxgl.Marker[]>([])
  const prevMarkersRef = useRef<MapMarker[]>([])
  const prevSelectedIdRef = useRef<string | undefined>(undefined)
  const onPickRef = useRef(onPick)
  onPickRef.current = onPick
  const onMarkerClickRef = useRef(onMarkerClick)
  onMarkerClickRef.current = onMarkerClick

  useEffect(() => {
    if (!el.current) return
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN
    if (!token) return

    mapboxgl.accessToken = token
    const initialCenter: [number, number] = center
      ? center
      : markers.length > 0
      ? [markers[0].longitude, markers[0].latitude]
      : [79.8612, 6.9271]

    const m = new mapboxgl.Map({
      container: el.current,
      style: darkTheme ? 'mapbox://styles/mapbox/dark-v11' : 'mapbox://styles/mapbox/streets-v12',
      center: initialCenter,
      zoom: zoom,
      doubleClickZoom: true,
      attributionControl: false,
    })

    m.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-left')
    m.addControl(new mapboxgl.AttributionControl({ compact: true }), 'bottom-right')

    if (onPickRef.current) {
      m.getCanvas().style.cursor = 'crosshair'
    }

    m.on('click', (e) => {
      onPickRef.current?.(e.lngLat.lat, e.lngLat.lng)
    })

    map.current = m

    const resizeObserver = new ResizeObserver(() => m.resize())
    resizeObserver.observe(el.current)
    setTimeout(() => m.resize(), 150)

    return () => {
      resizeObserver.disconnect()
      m.remove()
      map.current = null
      markersRef.current = []
      prevMarkersRef.current = []
      prevSelectedIdRef.current = undefined
    }
  }, [darkTheme])

  // Center update
  useEffect(() => {
    const m = map.current
    if (!m || !center) return
    m.flyTo({ center, zoom: zoom ?? Math.max(m.getZoom(), 14), duration: 800 })
  }, [center?.[0], center?.[1], zoom])

  // Update cursor if onPick toggles
  useEffect(() => {
    const m = map.current
    if (!m) return
    m.getCanvas().style.cursor = onPick ? 'crosshair' : ''
  }, [Boolean(onPick)])

  useEffect(() => {
    const m = map.current
    if (!m) return

    const prevMarkers = prevMarkersRef.current
    const markersChanged = !isSameMarkers(prevMarkers, markers)
    const selectionChanged = prevSelectedIdRef.current !== selectedId

    prevMarkersRef.current = markers
    prevSelectedIdRef.current = selectedId

    if (markersChanged) {
      markersRef.current.forEach((marker) => marker.remove())
      markersRef.current = []

      markers.forEach((p) => {
        const colour = COLOUR[p.tone ?? 'signal']
        const isSelected = selectedId === p.id
        const isTarget = p.tone === 'danger'

        const markerEl = document.createElement('div')
        if (isTarget) {
          // Tactical Target Pin with ping ripple
          markerEl.style.cssText = `
            position: relative;
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
          `
          const pingEl = document.createElement('div')
          pingEl.style.cssText = `
            position: absolute;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: rgba(239, 68, 68, 0.4);
            animation: ping 2s cubic-bezier(0, 0, 0.2, 1) infinite;
          `
          const coreEl = document.createElement('div')
          coreEl.style.cssText = `
            position: relative;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: #ef4444;
            border: 2.5px solid #ffffff;
            box-shadow: 0 0 14px rgba(239, 68, 68, 0.8), 0 0 0 2px rgba(0,0,0,0.4);
          `
          markerEl.appendChild(pingEl)
          markerEl.appendChild(coreEl)
        } else {
          markerEl.style.cssText = `
            width: ${isSelected ? 26 : 20}px;
            height: ${isSelected ? 26 : 20}px;
            border-radius: 50%;
            background: ${colour};
            border: ${isSelected ? 3 : 2}px solid #ffffff;
            box-shadow: 0 0 0 2px rgba(0,0,0,0.3), 0 0 10px ${colour}99;
            cursor: pointer;
            transition: transform 0.15s ease;
          `
        }

        const markerObj = new mapboxgl.Marker({ element: markerEl, anchor: 'center' })
          .setLngLat([p.longitude, p.latitude])
          .addTo(m)

        if (p.label) {
          markerEl.addEventListener('mouseenter', () => {
            new mapboxgl.Popup({ offset: 16, closeButton: false, className: 'tactical-tooltip' })
              .setLngLat([p.longitude, p.latitude])
              .setHTML(`<div style="color:#0f172a;font-family:Inter,sans-serif;font-size:12px;font-weight:600;padding:5px 9px;background:#ffffff;border:1px solid #cbd5e1;border-radius:6px;white-space:nowrap;box-shadow:0 4px 12px rgba(0,0,0,0.15);">${p.label}</div>`)
              .addTo(m)
          })
          markerEl.addEventListener('mouseleave', () => {
            m.getCanvas().style.cursor = onPickRef.current ? 'crosshair' : ''
          })
        }

        markerEl.addEventListener('click', (e) => {
          e.stopPropagation()
          onMarkerClickRef.current?.(p)
        })
        markersRef.current.push(markerObj)
      })
    } else if (selectionChanged) {
      markersRef.current.forEach((markerObj, index) => {
        const p = markers[index]
        if (!p || p.tone === 'danger') return
        const isSelected = selectedId === p.id
        const colour = COLOUR[p.tone ?? 'signal']
        const markerEl = markerObj.getElement()
        markerEl.style.width = `${isSelected ? 26 : 20}px`
        markerEl.style.height = `${isSelected ? 26 : 20}px`
        markerEl.style.border = `${isSelected ? 3 : 2}px solid #ffffff`
        markerEl.style.boxShadow = `0 0 0 2px rgba(0,0,0,0.3), 0 0 10px ${colour}99`
      })
    }
  }, [markers, selectedId])

  useEffect(() => {
    const m = map.current
    if (!m || !selectedId) return

    const target = markers.find((p) => p.id === selectedId)
    if (!target) return

    m.flyTo({ center: [target.longitude, target.latitude], zoom: 15, duration: 1000 })
  }, [selectedId, markers])

  const containerHeight = typeof height === 'number' ? `${height}px` : height

  return (
    <div
      ref={el}
      role="application"
      aria-label={label}
      style={{ height: containerHeight }}
      className={className ?? 'w-full overflow-hidden rounded-control border border-line'}
    />
  )
}
