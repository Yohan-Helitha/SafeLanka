import { useState } from 'react'
import { useToast } from '@/context/ToastContext'
import { useOnlineStatus } from '@/hooks/shared'
import { useRecordDistribution } from '@/hooks/response/useResponse'
import type { Allocation } from '@/types'
import { newClientRef } from '@/utils/id'
import { ApiErrorNotice } from '../domain'
import { Button, Dialog, Field, Input } from '../ui'

/** Mount only while open. Works offline: the entry is queued and sent when the connection returns. */
export function DistributionDialog({ allocation, onClose }: { allocation: Allocation; onClose: () => void }) {
  const { toast } = useToast()
  const online = useOnlineStatus()
  const record = useRecordDistribution()
  const remaining = allocation.quantity - allocation.distributed
  const [quantity, setQuantity] = useState('')
  const n = Number(quantity)
  const error = quantity && (!Number.isInteger(n) || n <= 0 || n > remaining) ? `Enter a whole number from 1 to ${remaining}.` : null

  return (
    <Dialog
      open
      onClose={onClose}
      title="Record distribution"
      description={`${allocation.itemName} · ${remaining} ${allocation.unit} left to hand out`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            loading={record.isPending}
            disabled={!quantity || Boolean(error)}
            onClick={() =>
              record.mutate(
                {
                  allocationId: allocation.id,
                  label: `${n} ${allocation.unit} of ${allocation.itemName.toLowerCase()}`,
                  input: { quantityDistributed: n, clientRef: newClientRef(), distributedAt: new Date().toISOString(), recordedOffline: !online },
                },
                {
                  onSuccess: (r) => {
                    toast(r.queued ? 'Distribution saved on this phone; it will send when you are back online' : 'Distribution recorded')
                    onClose()
                  },
                },
              )
            }
          >
            Record distribution
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Quantity handed out" required error={error} hint={`Up to ${remaining} ${allocation.unit}.`}>
          {(p) => <Input {...p} type="number" inputMode="numeric" min={1} max={remaining} value={quantity} onChange={(e) => setQuantity(e.target.value)} />}
        </Field>
        <ApiErrorNotice error={record.error} />
      </div>
    </Dialog>
  )
}
