import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiErrorNotice } from '@/components/domain'
import { LocationPicker } from '@/components/reports/LocationPicker'
import { PhotoPicker } from '@/components/reports/PhotoPicker'
import { Button, CharCount, Field, PageHeader, Segmented, Select, TextArea } from '@/components/ui'
import { LIMITS } from '@/constants/app'
import { CATEGORY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useGeolocation, useReferenceData } from '@/hooks/shared'
import { useSubmitReport } from '@/hooks/reports/useReports'
import type { ReportInput } from '@/types'
import { formatCoords, newClientRef } from '@/utils'
import { compact, lengthError } from '@/utils/validation'

export function ReportHazardScreen() {
  useDocumentTitle('Report a hazard')
  const user = useCurrentUser()
  const navigate = useNavigate()
  const ref = useReferenceData()
  const geo = useGeolocation()
  const submit = useSubmitReport()

  const [step, setStep] = useState<'form' | 'review'>('form')
  const [attempted, setAttempted] = useState(false)
  const [hazardTypeId, setHazardTypeId] = useState<string | null>(null)
  const [category, setCategory] = useState<string | null>(null)
  const [description, setDescription] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [manual, setManual] = useState(false)
  const [manualText, setManualText] = useState('')
  const [districtId, setDistrictId] = useState(user.districtId)

  const type = ref.activeHazardTypes.find((t) => t.id === hazardTypeId)
  const useManual = manual || geo.status === 'unavailable'

  const errors = compact({
    hazardTypeId: hazardTypeId ? null : 'Choose what is happening.',
    category: category ? null : 'Choose what you see.',
    description: lengthError('Description', description, LIMITS.description.min, LIMITS.description.max),
    location: useManual
      ? manualText.trim().length >= 5
        ? null
        : 'Describe the place in at least 5 characters.'
      : geo.fix
        ? null
        : 'Use your location or describe the place.',
    photo: photo && photo.size > LIMITS.photoBytes ? 'This photo is larger than 5 MB.' : null,
  })
  const show = (key: string) => (attempted ? (errors[key] ?? null) : null)

  const review = () => {
    setAttempted(true)
    if (Object.keys(errors).length === 0) setStep('review')
  }

  const send = () => {
    const input: ReportInput = {
      clientRef: newClientRef(),
      hazardTypeId: hazardTypeId!,
      category: category!,
      description: description.trim(),
      latitude: useManual ? null : geo.fix!.latitude,
      longitude: useManual ? null : geo.fix!.longitude,
      isManualLocation: useManual,
      manualLocationText: useManual ? manualText.trim() : null,
      districtId,
      capturedAt: new Date().toISOString(),
      photo,
    }
    submit.mutate(input, {
      onSuccess: (result) =>
        result.queued
          ? navigate(paths.citizen.reportDone(result.clientRef), { state: { kind: 'queued' } })
          : navigate(paths.citizen.reportDone(result.report.id), {
              state: { kind: 'sent', referenceNo: result.report.referenceNo },
            }),
    })
  }

  if (step === 'review') {
    return (
      <div>
        <PageHeader title="Check your report" subtitle="A DMC duty officer reviews it before it can affect any warning." />
        <dl className="divide-y divide-line rounded-card border border-line bg-panel text-[15px]">
          <Row label="Hazard" value={type?.name} />
          <Row label="What you saw" value={category ? CATEGORY_LABEL[category] : undefined} />
          <Row label="Description" value={description.trim()} />
          <Row
            label="Location"
            value={useManual ? manualText.trim() : geo.fix ? formatCoords(geo.fix.latitude, geo.fix.longitude) : undefined}
          />
          <Row label="District" value={ref.districtName(districtId)} />
          <Row label="Photo" value={photo ? undefined : 'None'}>
            {photo && <img src={URL.createObjectURL(photo)} alt="Photo attached to the report" className="mt-1 h-24 rounded-control object-cover" />}
          </Row>
        </dl>
        <div className="mt-4 space-y-3">
          <ApiErrorNotice error={submit.error} />
          <div className="grid grid-cols-2 gap-3">
            <Button variant="secondary" size="lg" onClick={() => setStep('form')}>
              Edit
            </Button>
            <Button size="lg" loading={submit.isPending} onClick={send}>
              Send report
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        review()
      }}
      className="space-y-5"
    >
      <PageHeader title="Report a hazard" subtitle="Tell the Disaster Management Centre what you see." />
      <Segmented
        legend="What is happening?"
        value={hazardTypeId}
        onChange={(v) => {
          setHazardTypeId(v)
          setCategory(null)
        }}
        options={ref.activeHazardTypes.map((t) => ({ value: t.id, label: t.name }))}
        error={show('hazardTypeId')}
      />
      {type && (
        <Segmented
          legend="What do you see?"
          columns={1}
          value={category}
          onChange={setCategory}
          options={type.reportCategories.map((c) => ({ value: c, label: CATEGORY_LABEL[c] ?? c }))}
          error={show('category')}
        />
      )}
      <Field
        label="Describe it"
        required
        error={show('description')}
        hint="Where exactly, how fast it is changing, who is at risk."
        aside={<CharCount value={description} max={LIMITS.description.max} />}
      >
        {(p) => <TextArea {...p} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Water is over the road near Kolonnawa station and rising." />}
      </Field>
      <div>
        <p className="mb-1.5 text-sm font-medium text-ink">Photo (optional)</p>
        <PhotoPicker file={photo} onChange={setPhoto} error={show('photo')} />
      </div>
      <fieldset>
        <legend className="mb-1.5 text-sm font-medium text-ink">Location</legend>
        <LocationPicker
          geo={geo}
          manual={manual}
          onManualChange={setManual}
          manualText={manualText}
          onManualTextChange={setManualText}
          error={show('location')}
        />
      </fieldset>
      <Field label="District" required>
        {(p) => (
          <Select {...p} value={districtId} onChange={(e) => setDistrictId(e.target.value)}>
            {ref.districts.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </Select>
        )}
      </Field>
      <Button type="submit" size="lg" block>
        Review report
      </Button>
    </form>
  )
}

function Row({ label, value, children }: { label: string; value?: string; children?: React.ReactNode }) {
  return (
    <div className="px-4 py-3">
      <dt className="text-sm text-muted">{label}</dt>
      <dd className="text-ink">
        {value}
        {children}
      </dd>
    </div>
  )
}
