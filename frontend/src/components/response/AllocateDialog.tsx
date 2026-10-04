import { useState } from 'react'
import { useToast } from '@/context/ToastContext'
import { useAllocate, useShelters } from '@/hooks/response/useResponse'
import { isApiError } from '@/services'
import type { ReliefStock } from '@/types'
import { ApiErrorNotice } from '../domain'
import { Button, Dialog, Field, Input, Select } from '../ui'

interface Alt {
  stockId: string
  organisationName: string
  districtName: string
  quantityAvailable: number
}

interface Props {
  stock: ReliefStock
  districtId: string
  eventId: string
  onClose: () => void
}

/** Mount only while open. Shows the shortfall and other stock lines when there is not enough. */
export function AllocateDialog({ stock, districtId, eventId, onClose }: Props) {
  const { toast } = useToast()
  const shelters = useShelters({ districtId })
  const allocate = useAllocate()
  const [shelterId, setShelterId] = useState('')
  const [quantity, setQuantity] = useState('')
  const n = Number(quantity)
  const valid = shelterId && Number.isInteger(n) && n > 0

  const stockError = isApiError(allocate.error) && allocate.error.code === 'INSUFFICIENT_STOCK' ? allocate.error : null
  const alternatives = (stockError?.details?.alternatives as Alt[] | undefined) ?? []

  return (
    <Dialog
      open
      onClose={onClose}
      title="Allocate stock"
      description={`${stock.itemName} from ${stock.organisationName} · ${stock.quantityAvailable} ${stock.unit} available`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={!valid}
            loading={allocate.isPending}
            onClick={() =>
              allocate.mutate(
                { stockId: stock.id, shelterId, eventId, quantity: n },
                {
                  onSuccess: () => {
                    toast('Stock allocated')
                    onClose()
                  },
                },
              )
            }
          >
            Allocate stock
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field label="Shelter" required>
          {(p) => (
            <Select {...p} value={shelterId} onChange={(e) => setShelterId(e.target.value)}>
              <option value="">Choose a shelter</option>
              {(shelters.data ?? [])
                .filter((s) => s.status !== 'CLOSED')
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
            </Select>
          )}
        </Field>
        <Field label={`Quantity (${stock.unit})`} required>
          {(p) => <Input {...p} type="number" inputMode="numeric" min={1} value={quantity} onChange={(e) => setQuantity(e.target.value)} />}
        </Field>
        <ApiErrorNotice error={allocate.error}>
          {stockError && (
            <p className="mt-1 text-muted">
              Short by {String(stockError.details?.shortfall)} {stock.unit}.
              {alternatives.length > 0 ? ' Other stock lines that cover it:' : ' No other line covers this quantity.'}
            </p>
          )}
          {alternatives.length > 0 && (
            <ul className="mt-1 list-disc pl-4 text-muted">
              {alternatives.map((a) => (
                <li key={a.stockId}>
                  {a.organisationName}, {a.districtName}: {a.quantityAvailable} {stock.unit}
                </li>
              ))}
            </ul>
          )}
        </ApiErrorNotice>
      </div>
    </Dialog>
  )
}
