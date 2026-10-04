import { useState } from 'react'
import { LIMITS } from '@/constants/app'
import { useCreateHazard } from '@/hooks/warnings/useWarnings'
import { useReferenceData } from '@/hooks/shared'
import { compact, lengthError } from '@/utils/validation'
import { ApiErrorNotice } from '../domain'
import { Button, CharCount, Dialog, Field, Segmented, Select, TextArea } from '../ui'

interface Props {
  onClose: () => void
  onCreated: (hazardId: string) => void
}

/** Mount only while open so the form starts fresh each time. */
export function RecordHazardDialog({ onClose, onCreated }: Props) {
  const ref = useReferenceData()
  const create = useCreateHazard()
  const [hazardTypeId, setHazardTypeId] = useState('')
  const [area, setArea] = useState('')
  const [severity, setSeverity] = useState<number | null>(3)
  const [description, setDescription] = useState('')
  const [attempted, setAttempted] = useState(false)

  const errors = compact({
    hazardTypeId: hazardTypeId ? null : 'Choose a hazard type.',
    area: area ? null : 'Choose where it is happening.',
    description: lengthError('Description', description, LIMITS.description.min, LIMITS.description.max),
  })
  const show = (k: string) => (attempted ? (errors[k] ?? null) : null)

  const save = () => {
    setAttempted(true)
    if (Object.keys(errors).length) return
    const [kind, id] = area.split(':')
    create.mutate(
      {
        hazardTypeId,
        severity: severity ?? 3,
        districtId: kind === 'd' ? id : null,
        riverBasinId: kind === 'b' ? id : null,
        description: description.trim(),
      },
      { onSuccess: (hazard) => onCreated(hazard.id) },
    )
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Record hazard"
      description="Add a hazard you have seen or been told about. It starts as under assessment."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={create.isPending} onClick={save}>
            Record hazard
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field label="Hazard type" required error={show('hazardTypeId')}>
          {(p) => (
            <Select {...p} value={hazardTypeId} onChange={(e) => setHazardTypeId(e.target.value)}>
              <option value="">Choose a type</option>
              {ref.activeHazardTypes.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Area" required error={show('area')}>
          {(p) => (
            <Select {...p} value={area} onChange={(e) => setArea(e.target.value)}>
              <option value="">Choose an area</option>
              <optgroup label="Districts">
                {ref.districts.map((d) => (
                  <option key={d.id} value={`d:${d.id}`}>
                    {d.name}
                  </option>
                ))}
              </optgroup>
              <optgroup label="River basins">
                {ref.riverBasins.map((b) => (
                  <option key={b.id} value={`b:${b.id}`}>
                    {b.name}
                  </option>
                ))}
              </optgroup>
            </Select>
          )}
        </Field>
        <Segmented
          legend="Severity (1 low, 5 extreme)"
          columns={5}
          value={severity}
          onChange={setSeverity}
          options={[1, 2, 3, 4, 5].map((n) => ({ value: n, label: String(n) }))}
        />
        <Field label="Description" required error={show('description')} aside={<CharCount value={description} max={LIMITS.description.max} />}>
          {(p) => <TextArea {...p} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Kelani river overtopping at Kolonnawa, roads closed." />}
        </Field>
        <ApiErrorNotice error={create.error} />
      </div>
    </Dialog>
  )
}
