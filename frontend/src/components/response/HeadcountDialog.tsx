import { Minus, Plus } from 'lucide-react'
import { useState } from 'react'
import { useToast } from '@/context/ToastContext'
import { useUpdateOccupancy } from '@/hooks/response/useResponse'
import type { Shelter } from '@/types'
import { ApiErrorNotice, OccupancyBar } from '../domain'
import { Button, Dialog } from '../ui'

/** Mount only while open. Stepper with a live occupancy bar; blocks counts above capacity. */
export function HeadcountDialog({ shelter, onClose }: { shelter: Shelter; onClose: () => void }) {
  const { toast } = useToast()
  const update = useUpdateOccupancy()
  const [count, setCount] = useState(shelter.currentOccupancy)
  const over = count > shelter.capacity

  return (
    <Dialog
      open
      onClose={onClose}
      title="Update headcount"
      description={`${shelter.name} holds ${shelter.capacity} people.`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={over || count < 0}
            loading={update.isPending}
            onClick={() =>
              update.mutate(
                { shelterId: shelter.id, occupancy: count },
                {
                  onSuccess: () => {
                    toast('Headcount updated')
                    onClose()
                  },
                },
              )
            }
          >
            Save headcount
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="flex items-center justify-center gap-3">
          <Button variant="secondary" size="lg" aria-label="One fewer person" onClick={() => setCount((c) => Math.max(0, c - 1))} icon={<Minus className="size-5" aria-hidden />} />
          <input
            aria-label="People in the shelter"
            type="number"
            inputMode="numeric"
            min={0}
            value={count}
            onChange={(e) => setCount(Math.max(0, Math.floor(Number(e.target.value) || 0)))}
            className="tabular h-[52px] w-28 rounded-control border border-line bg-raised text-center font-display text-3xl font-semibold text-ink"
          />
          <Button variant="secondary" size="lg" aria-label="One more person" onClick={() => setCount((c) => c + 1)} icon={<Plus className="size-5" aria-hidden />} />
        </div>
        <OccupancyBar occupancy={Math.min(count, shelter.capacity)} capacity={shelter.capacity} />
        {over && <p className="text-sm text-danger">That is {count - shelter.capacity} more than the shelter holds. Lower the number or move people to another shelter.</p>}
        <ApiErrorNotice error={update.error} />
      </div>
    </Dialog>
  )
}
