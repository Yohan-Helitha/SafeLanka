import { Megaphone, Plus } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SeverityBadge, StatusChip } from '@/components/domain'
import { Button, DataTable, EmptyState, ErrorState, Loading, PageHeader, Segmented } from '@/components/ui'
import { paths } from '@/constants/routes'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useWarningsList } from '@/hooks/warnings/useWarnings'
import type { WarningListItem, WarningStatus } from '@/types'
import { formatDateTime, formatNumber } from '@/utils/format'

type Filter = WarningStatus | 'ALL'

export function WarningsScreen() {
  useDocumentTitle('Warnings')
  const navigate = useNavigate()
  const { districtName } = useReferenceData()
  const [filter, setFilter] = useState<Filter>('ACTIVE')
  const warnings = useWarningsList(filter)

  return (
    <div>
      <PageHeader
        title="Warnings"
        subtitle="Official warnings issued to the public."
        actions={
          <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => navigate(paths.dmc.newWarning)}>
            New warning
          </Button>
        }
      />
      <Segmented<Filter>
        legend="Show warnings"
        hideLegend
        columns={4}
        className="mb-4 max-w-xl"
        value={filter}
        onChange={setFilter}
        options={[
          { value: 'ACTIVE', label: 'Active' },
          { value: 'ESCALATED', label: 'Escalated' },
          { value: 'CANCELLED', label: 'Cancelled' },
          { value: 'ALL', label: 'All' },
        ]}
      />
      {warnings.isLoading && <Loading />}
      {warnings.isError && <ErrorState error={warnings.error} onRetry={() => void warnings.refetch()} />}
      {warnings.data && (
        <DataTable<WarningListItem>
          caption="Warnings, newest first"
          rows={warnings.data}
          rowKey={(w) => w.id}
          onRowClick={(w) => navigate(paths.dmc.warning(w.id))}
          empty={<EmptyState icon={Megaphone} title="No warnings here" description="Change the filter, or issue a new warning." />}
          columns={[
            { key: 'level', header: 'Level', cell: (w) => <SeverityBadge level={w.level} size="sm" /> },
            { key: 'title', header: 'Warning', cell: (w) => <span className="font-medium">{w.title}</span> },
            { key: 'districts', header: 'Districts', cell: (w) => <span className="text-muted">{w.districtIds.map(districtName).join(', ')}</span> },
            { key: 'reached', header: 'Reached', cell: (w) => <span className="tabular">{formatNumber(w.reached)} people</span> },
            { key: 'issued', header: 'Issued', cell: (w) => <span className="text-muted">{formatDateTime(w.issuedAt)}</span> },
            { key: 'status', header: 'Status', cell: (w) => <StatusChip status={w.status} /> },
          ]}
        />
      )}
    </div>
  )
}
