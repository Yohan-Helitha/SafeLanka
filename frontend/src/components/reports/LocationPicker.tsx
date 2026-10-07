import { LocateFixed, MapPinOff } from 'lucide-react'
import type { useGeolocation } from '@/hooks/shared'
import { formatCoords } from '@/utils/format'
import { Button, Field, Input } from '../ui'

interface Props {
  geo: ReturnType<typeof useGeolocation>
  manual: boolean
  onManualChange: (manual: boolean) => void
  manualText: string
  onManualTextChange: (text: string) => void
  error?: string | null
}

/** GPS fix, or a typed place description when GPS is not available (alternative flow A2). */
export function LocationPicker({ geo, manual, onManualChange, manualText, onManualTextChange, error }: Props) {
  const useManual = manual || geo.status === 'unavailable'

  if (useManual) {
    return (
      <div className="space-y-3 rounded-control border border-caution/50 bg-caution/10 p-3">
        <p className="flex items-start gap-2 text-sm text-ink">
          <MapPinOff className="mt-0.5 size-4 shrink-0 text-caution" aria-hidden />
          <span>
            {geo.reason ?? 'You chose to describe the place.'} Tell the officer where this is.
          </span>
        </p>
        <Field label="Place description" required error={error} hint="Street, landmark and nearest town.">
          {(p) => (
            <Input
              {...p}
              value={manualText}
              onChange={(e) => onManualTextChange(e.target.value)}
              placeholder="Near Kolonnawa railway station, Station Road"
            />
          )}
        </Field>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => {
            onManualChange(false)
            geo.reset()
            geo.locate()
          }}
        >
          Try GPS again
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-3 rounded-control border border-line bg-raised p-3">
      <div className="flex items-start gap-3">
        <LocateFixed className="mt-0.5 size-5 shrink-0 text-signal" aria-hidden />
        <div className="min-w-0 flex-1">
          {geo.status === 'ok' && geo.fix ? (
            <p className="tabular font-medium text-ink">
              {formatCoords(geo.fix.latitude, geo.fix.longitude)}
              {geo.accuracyMetres !== null && <span className="ml-1 font-normal text-muted">± {geo.accuracyMetres} m</span>}
            </p>
          ) : geo.status === 'locating' ? (
            <p className="text-muted">Finding your location…</p>
          ) : (
            <p className="text-muted">Share your location so the officer can find the hazard.</p>
          )}
          {error && <p className="mt-1 text-sm text-danger">{error}</p>}
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" size="sm" loading={geo.status === 'locating'} onClick={geo.locate}>
          {geo.status === 'ok' ? 'Update location' : 'Use my location'}
        </Button>
        <Button variant="ghost" size="sm" onClick={() => onManualChange(true)}>
          Describe the location instead
        </Button>
        <Button
          variant="ghost"
          size="sm"
          aria-pressed={geo.simulateFailure}
          onClick={() => geo.setSimulateFailure(!geo.simulateFailure)}
        >
          {geo.simulateFailure ? 'GPS off (simulated)' : 'Simulate GPS off'}
        </Button>
      </div>
    </div>
  )
}
