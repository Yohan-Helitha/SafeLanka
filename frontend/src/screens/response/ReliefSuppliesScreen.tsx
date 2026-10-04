import { useState } from 'react'
import { StatusChip } from '@/components/domain'
import { AllocateDialog } from '@/components/response/AllocateDialog'
import { Button, Card, DataTable, ErrorState, Loading, PageHeader } from '@/components/ui'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAllocations, useStocks } from '@/hooks/response/useResponse'
import type { Allocation, ReliefStock } from '@/types'
import { formatNumber, relativeTime } from '@/utils/format'

export function ReliefSuppliesScreen() {
  useDocumentTitle('Relief supplies')
  const user = useCurrentUser()
  const { activeEvents } = useReferenceData()
  const stocks = useStocks(user.districtId)
  const allocations = useAllocations({ districtId: user.districtId })
  const [allocating, setAllocating] = useState<ReliefStock | null>(null)
  const event = activeEvents.find((e) => e.districtIds.includes(user.districtId)) ?? activeEvents[0]

  return (
    <div>
      <PageHeader title="Relief supplies" subtitle="Stock held in your district, and what has been sent to shelters." />
      <div className="space-y-5">
        <Card title="Stock" padded={false}>
          <div className="p-4 pt-3">
            {stocks.isLoading && <Loading />}
            {stocks.isError && <ErrorState error={stocks.error} onRetry={() => void stocks.refetch()} />}
            {stocks.data && (
              <DataTable<ReliefStock>
                caption="Relief stock in this district"
                rows={stocks.data}
                rowKey={(s) => s.id}
                empty={<p className="py-6 text-center text-muted">No stock is held in this district.</p>}
                columns={[
                  { key: 'item', header: 'Item', cell: (s) => <span className="font-medium">{s.itemName}</span> },
                  { key: 'owner', header: 'Owner', cell: (s) => s.organisationName },
                  { key: 'qty', header: 'Available', align: 'right', cell: (s) => <span className="tabular">{formatNumber(s.quantityAvailable)} {s.unit}</span> },
                  {
                    key: 'act',
                    header: '',
                    align: 'right',
                    cell: (s) => (
                      <Button size="sm" variant="secondary" disabled={!event || s.quantityAvailable === 0} onClick={() => setAllocating(s)}>
                        Allocate
                      </Button>
                    ),
                  },
                ]}
              />
            )}
          </div>
        </Card>

        <Card title="Allocations" padded={false}>
          <div className="p-4 pt-3">
            {allocations.isLoading && <Loading />}
            {allocations.data && (
              <DataTable<Allocation>
                caption="Allocations to shelters, newest first"
                rows={allocations.data}
                rowKey={(a) => a.id}
                empty={<p className="py-6 text-center text-muted">Nothing has been allocated yet.</p>}
                columns={[
                  { key: 'item', header: 'Item', cell: (a) => <span className="font-medium">{a.itemName}</span> },
                  { key: 'shelter', header: 'Shelter', cell: (a) => a.shelterName },
                  { key: 'qty', header: 'Handed out', align: 'right', cell: (a) => <span className="tabular">{a.distributed} of {a.quantity} {a.unit}</span> },
                  { key: 'status', header: 'Status', cell: (a) => <StatusChip status={a.status} /> },
                  { key: 'when', header: 'Allocated', cell: (a) => <span className="text-muted">{relativeTime(a.allocatedAt)}</span> },
                ]}
              />
            )}
          </div>
        </Card>
      </div>
      {allocating && event && <AllocateDialog stock={allocating} districtId={user.districtId} eventId={event.id} onClose={() => setAllocating(null)} />}
    </div>
  )
}
