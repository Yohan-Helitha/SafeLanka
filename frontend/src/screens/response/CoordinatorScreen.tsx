import { School } from 'lucide-react'
import { useState } from 'react'
import { DistributionDialog } from '@/components/response/DistributionDialog'
import { HeadcountDialog } from '@/components/response/HeadcountDialog'
import { ShelterCard } from '@/components/response/ShelterCard'
import { Button, Card, EmptyState, ErrorState, Loading, PageHeader } from '@/components/ui'
import { useCurrentUser } from '@/context/ActingUserContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAllocations, useShelters } from '@/hooks/response/useResponse'
import type { Allocation, Shelter } from '@/types'

export function CoordinatorScreen() {
  useDocumentTitle('My shelter')
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const shelters = useShelters({ coordinatorId: user.id })
  const allocations = useAllocations({ districtId: user.districtId })
  const [counting, setCounting] = useState<Shelter | null>(null)
  const [recording, setRecording] = useState<Allocation | null>(null)

  if (shelters.isLoading) return <Loading />
  if (shelters.isError) return <ErrorState error={shelters.error} onRetry={() => void shelters.refetch()} />
  if (!shelters.data?.length) {
    return (
      <div>
        <PageHeader title="My shelter" />
        <EmptyState icon={School} title="No shelter assigned to you" description="Ask your district officer to link you to a shelter." />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader title="My shelter" subtitle="Keep the headcount current and record relief you hand out." />
      {shelters.data.map((shelter) => {
        const open = (allocations.data ?? []).filter(
          (a) => a.shelterId === shelter.id && a.status !== 'DISTRIBUTED' && a.status !== 'CANCELLED',
        )
        return (
          <div key={shelter.id} className="space-y-4">
            <ShelterCard
              shelter={shelter}
              size="lg"
              districtName={districtName(shelter.districtId)}
              actions={
                <Button size="lg" block onClick={() => setCounting(shelter)}>
                  Update headcount
                </Button>
              }
            />
            <Card title="Relief to hand out">
              {allocations.isLoading && <Loading />}
              {allocations.data && open.length === 0 && <p className="text-sm text-muted">Nothing waiting to be handed out.</p>}
              <ul className="space-y-3">
                {open.map((a) => (
                  <li key={a.id} className="flex items-center justify-between gap-3 rounded-control border border-line bg-raised p-3">
                    <p className="text-[15px] text-ink">
                      <span className="font-medium">{a.itemName}</span>
                      <span className="tabular block text-sm text-muted">
                        {a.distributed} of {a.quantity} {a.unit} handed out · from {a.organisationName}
                      </span>
                    </p>
                    <Button size="md" variant="secondary" onClick={() => setRecording(a)}>
                      Record
                    </Button>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        )
      })}
      {counting && <HeadcountDialog shelter={counting} onClose={() => setCounting(null)} />}
      {recording && <DistributionDialog allocation={recording} onClose={() => setRecording(null)} />}
    </div>
  )
}
