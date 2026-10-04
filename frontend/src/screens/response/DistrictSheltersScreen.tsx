import { School } from 'lucide-react'
import { useState } from 'react'
import { HeadcountDialog } from '@/components/response/HeadcountDialog'
import { ShelterCard } from '@/components/response/ShelterCard'
import { Button, EmptyState, ErrorState, LocationMap, Loading, PageHeader } from '@/components/ui'
import { useCurrentUser } from '@/context/ActingUserContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useShelters } from '@/hooks/response/useResponse'
import type { Shelter } from '@/types'

export function DistrictSheltersScreen() {
  useDocumentTitle('Shelters')
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const shelters = useShelters({ districtId: user.districtId })
  const [counting, setCounting] = useState<Shelter | null>(null)

  return (
    <div>
      <PageHeader title="Shelters" subtitle="Occupancy across the shelters in your district. Amber starts at 90% full." />
      {shelters.isLoading && <Loading />}
      {shelters.isError && <ErrorState error={shelters.error} onRetry={() => void shelters.refetch()} />}
      {shelters.data?.length === 0 && <EmptyState icon={School} title="No shelters in this district" />}
      {shelters.data && shelters.data.length > 0 && (
        <div className="mb-4">
          <LocationMap
            height={260}
            label="Map of shelters in this district"
            markers={shelters.data.map((s) => ({
              id: s.id,
              latitude: s.latitude,
              longitude: s.longitude,
              label: `${s.name}: ${s.currentOccupancy}/${s.capacity}`,
              tone: s.level === 'FULL' ? 'danger' : s.level === 'AMBER' ? 'caution' : 'ok',
            }))}
          />
        </div>
      )}
      <div className="grid gap-4 md:grid-cols-2">
        {shelters.data?.map((s) => (
          <ShelterCard
            key={s.id}
            shelter={s}
            districtName={districtName(s.districtId)}
            actions={
              s.status === 'CLOSED' ? undefined : (
                <Button variant="secondary" size="sm" onClick={() => setCounting(s)}>
                  Update headcount
                </Button>
              )
            }
          />
        ))}
      </div>
      {counting && <HeadcountDialog shelter={counting} onClose={() => setCounting(null)} />}
    </div>
  )
}
