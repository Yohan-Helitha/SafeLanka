import { Plus, Radar } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SeverityMeter, StatusChip } from '@/components/domain'
import { Button, Checkbox, DataTable, EmptyState, ErrorState, Loading, PageHeader } from '@/components/ui'
import { RecordHazardDialog } from '@/components/warnings/RecordHazardDialog'
import { HAZARD_SOURCE_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useHazards } from '@/hooks/warnings/useWarnings'
import type { HazardListItem } from '@/types'
import { plural, relativeTime } from '@/utils/format'

export function HazardDashboardScreen() {
  useDocumentTitle('Hazards')
  const navigate = useNavigate()
  const { hazardTypeName, areaName } = useReferenceData()
  const [showResolved, setShowResolved] = useState(false)
  const [recording, setRecording] = useState(false)
  const hazards = useHazards(showResolved)

  return (
    <div>
      <PageHeader
        title="Hazards"
        subtitle="Assess each hazard with verified reports and gauge readings before issuing a public warning."
        actions={
          <>
            <Checkbox label="Show resolved" checked={showResolved} onChange={(e) => setShowResolved(e.target.checked)} />
            <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => setRecording(true)}>
              Record hazard
            </Button>
          </>
        }
      />

      {hazards.isLoading && <Loading />}
      {hazards.isError && <ErrorState error={hazards.error} onRetry={() => void hazards.refetch()} />}
      {hazards.data && (
        <DataTable<HazardListItem>
          caption="Hazards, highest severity first"
          rows={hazards.data}
          rowKey={(h) => h.id}
          minWidth={820}
          onRowClick={(h) => navigate(paths.dmc.hazard(h.id))}
          empty={
            <EmptyState
              icon={Radar}
              title="No open hazards"
              description="Record a hazard, or wait for a gauge or verified report to open one."
              action={<Button onClick={() => setRecording(true)}>Record hazard</Button>}
            />
          }
          columns={[
            { key: 'sev', header: 'Severity', cell: (h) => <SeverityMeter value={h.severity} /> },
            { key: 'type', header: 'Hazard type', cell: (h) => <span className="font-medium">{hazardTypeName(h.hazardTypeId)}</span> },
            { key: 'area', header: 'Area', cell: (h) => areaName(h.districtId, h.riverBasinId) },
            { key: 'source', header: 'Source', cell: (h) => HAZARD_SOURCE_LABEL[h.source] },
            {
              key: 'evidence',
              header: 'Evidence',
              cell: (h) => (
                <span className="text-sm">
                  {h.verifiedReportCount > 0 && <span className="block">{plural(h.verifiedReportCount, 'verified report')}</span>}
                  {h.latestReading && (
                    <span className={`tabular block ${h.latestReading.aboveAlert ? 'text-caution' : 'text-muted'}`}>
                      Gauge {h.latestReading.value.toFixed(2)} {h.latestReading.unit}
                      {h.latestReading.aboveAlert && ' · above alert'}
                    </span>
                  )}
                  {h.verifiedReportCount === 0 && !h.latestReading && <span className="text-faint">None yet</span>}
                </span>
              ),
            },
            { key: 'detected', header: 'Detected', cell: (h) => <span className="text-muted">{relativeTime(h.detectedAt)}</span> },
            { key: 'status', header: 'Status', cell: (h) => <StatusChip status={h.status} /> },
          ]}
        />
      )}

      {recording && (
        <RecordHazardDialog
          onClose={() => setRecording(false)}
          onCreated={(id) => {
            setRecording(false)
            navigate(paths.dmc.hazard(id))
          }}
        />
      )}
    </div>
  )
}
