const TZ = 'Asia/Colombo'

const dateTimeFmt = new Intl.DateTimeFormat('en-GB', {
  timeZone: TZ,
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})
const dateFmt = new Intl.DateTimeFormat('en-GB', {
  timeZone: TZ,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})
const timeFmt = new Intl.DateTimeFormat('en-GB', {
  timeZone: TZ,
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})
const shortDayFmt = new Intl.DateTimeFormat('en-GB', { timeZone: TZ, day: 'numeric', month: 'short' })

/** "4 Oct, 14:05" in Sri Lanka time. */
export function formatDateTime(iso: string | null | undefined): string {
  return iso ? dateTimeFmt.format(new Date(iso)).replace(/,?\s(?=\d{2}:\d{2}$)/, ', ') : '—'
}

/** "14 May 2026". */
export function formatDate(iso: string | null | undefined): string {
  return iso ? dateFmt.format(new Date(iso)) : '—'
}

/** "14:05". */
export function formatTime(iso: string | null | undefined): string {
  return iso ? timeFmt.format(new Date(iso)) : '—'
}

/** "4 Oct". */
export function formatShortDay(iso: string): string {
  return shortDayFmt.format(new Date(iso))
}

/** "12 min ago" for recent times, an absolute date after a week. */
export function relativeTime(iso: string | null | undefined, now = Date.now()): string {
  if (!iso) return '—'
  const diff = now - new Date(iso).getTime()
  if (diff < 0) return formatDateTime(iso)
  const min = Math.floor(diff / 60_000)
  if (min < 1) return 'just now'
  if (min < 60) return `${min} min ago`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h} h ago`
  const d = Math.floor(h / 24)
  if (d < 7) return `${d} d ago`
  return formatDateTime(iso)
}

export function formatNumber(n: number, digits = 0): string {
  return n.toLocaleString('en-GB', { maximumFractionDigits: digits, minimumFractionDigits: digits })
}

export function formatPercent(ratio: number): string {
  return `${Math.round(ratio * 100)}%`
}

export function formatMinutes(min: number | null): string {
  if (min == null) return '—'
  if (min < 60) return `${min} min`
  const h = Math.floor(min / 60)
  return `${h} h ${min % 60} min`
}

/** "NEEDS_MORE_INFO" -> "Needs more info". */
export function humanise(value: string): string {
  const s = value.replace(/_/g, ' ').toLowerCase()
  return s.charAt(0).toUpperCase() + s.slice(1)
}

/** "6.9391° N, 79.8921° E". */
export function formatCoords(lat: number, lng: number): string {
  return `${Math.abs(lat).toFixed(4)}° ${lat >= 0 ? 'N' : 'S'}, ${Math.abs(lng).toFixed(4)}° ${lng >= 0 ? 'E' : 'W'}`
}

export function plural(n: number, one: string, many = `${one}s`): string {
  return `${formatNumber(n)} ${n === 1 ? one : many}`
}

/** Value for <input type="datetime-local"> in Sri Lanka time. */
export function toLocalInput(iso: string): string {
  const parts = new Intl.DateTimeFormat('sv-SE', {
    timeZone: TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
  return parts.replace(' ', 'T')
}

/** Converts a datetime-local value (Sri Lanka time, UTC+05:30) to an ISO instant. */
export function fromLocalInput(value: string): string {
  return new Date(`${value}:00+05:30`).toISOString()
}

export function fileStamp(iso: string): string {
  return iso.slice(0, 10)
}

export function slug(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}
