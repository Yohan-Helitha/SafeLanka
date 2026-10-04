import type { LatLng } from '@/types'

const R = 6371000

/** Great-circle distance in metres. */
export function haversineMetres(a: LatLng, b: LatLng): number {
  const rad = (d: number) => (d * Math.PI) / 180
  const dLat = rad(b.latitude - a.latitude)
  const dLng = rad(b.longitude - a.longitude)
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

export const SRI_LANKA_BOUNDS = { minLat: 5.8, maxLat: 9.9, minLng: 79.5, maxLng: 82.0 }

export function insideSriLanka(p: LatLng): boolean {
  return (
    p.latitude >= SRI_LANKA_BOUNDS.minLat &&
    p.latitude <= SRI_LANKA_BOUNDS.maxLat &&
    p.longitude >= SRI_LANKA_BOUNDS.minLng &&
    p.longitude <= SRI_LANKA_BOUNDS.maxLng
  )
}

/** Fallback fix used when a device reports a position outside Sri Lanka (demo only). */
export const COLOMBO_FIX: LatLng = { latitude: 6.9391, longitude: 79.8921 }

export function mapLink(p: LatLng): string {
  return `https://www.openstreetmap.org/?mlat=${p.latitude}&mlon=${p.longitude}#map=16/${p.latitude}/${p.longitude}`
}

export function directionsLink(p: LatLng): string {
  return `https://www.openstreetmap.org/directions?to=${p.latitude}%2C${p.longitude}`
}
