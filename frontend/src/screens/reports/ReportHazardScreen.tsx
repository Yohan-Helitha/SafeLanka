import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiErrorNotice } from '@/components/domain'
import { LocationPicker } from '@/components/reports/LocationPicker'
import { PhotoPicker } from '@/components/reports/PhotoPicker'
import { Button, CharCount, Field, PageHeader, Segmented, Select, TextArea } from '@/components/ui'
import { LIMITS } from '@/constants/app'
import { CATEGORY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useGeolocation, useReferenceData } from '@/hooks/shared'
import { removeDraft, saveDraft, useReportDrafts } from '@/hooks/reports/useReportDrafts'
import { useSubmitReport } from '@/hooks/reports/useReports'
import { draftStore } from '@/services/offline/drafts'
import type { ReportDraft } from '@/services/offline/drafts'
import type { LatLng, ReportInput } from '@/types'
import { formatCoords, newClientRef } from '@/utils'
import { compact, lengthError } from '@/utils/validation'

export function ReportHazardScreen() {
  useDocumentTitle('Report a hazard')
  const user = useCurrentUser()
  const navigate = useNavigate()
  const ref = useReferenceData()
  const geo = useGeolocation()
  const submit = useSubmitReport()

  const [searchParams] = useSearchParams()
  const resumeId = searchParams.get('draft')
  // The draft id is also the clientRef, so resending after a failure can never create a second report.
  const [draftId] = useState(() => resumeId ?? newClientRef())
  const [startedAt, setStartedAt] = useState(() => new Date().toISOString())
  const sent = useRef(false)
  const { drafts } = useReportDrafts()
  const otherDrafts = drafts.filter((d) => d.id !== draftId)

  const [loaded, setLoaded] = useState(!resumeId)
  const [step, setStep] = useState<'form' | 'review'>('form')
  const [attempted, setAttempted] = useState(false)
  const [hazardTypeId, setHazardTypeId] = useState<string | null>(null)
  const [category, setCategory] = useState<string | null>(null)
  const [description, setDescription] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [manual, setManual] = useState(false)
  const [manualText, setManualText] = useState('')
  const [districtId, setDistrictId] = useState(user.districtId)
  const [savedFix, setSavedFix] = useState<LatLng | null>(null)

  const type = ref.activeHazardTypes.find((t) => t.id === hazardTypeId)
  const useManual = manual || geo.status === 'unavailable'
  // A location saved with the draft stays valid until the person asks for a new one.
  const fix = geo.status === 'ok' && geo.fix ? geo.fix : savedFix
  const geoView =
    geo.status === 'idle' && savedFix && !manual
      ? { ...geo, status: 'ok' as const, fix: savedFix, accuracyMetres: null }
      : geo

  // Opening a saved report from "My reports" fills the form with what was saved.
  useEffect(() => {
    if (!resumeId) return
    let cancelled = false
    draftStore
      .get(resumeId)
      .then((d) => {
        if (cancelled || !d || d.ownerId !== user.id) return
        setStartedAt(d.createdAt)
        setHazardTypeId(d.hazardTypeId)
        setCategory(d.category)
        setDescription(d.description)
        setPhoto(d.photo ? new File([d.photo], 'photo', { type: d.photo.type }) : null)
        setManual(d.manual)
        setManualText(d.manualText)
        setDistrictId(d.districtId)
        setSavedFix(d.latitude !== null && d.longitude !== null ? { latitude: d.latitude, longitude: d.longitude } : null)
      })
      .catch(() => undefined)
      .finally(() => !cancelled && setLoaded(true))
    return () => {
      cancelled = true
    }
  }, [resumeId, user.id])

  const hasContent = Boolean(hazardTypeId || description.trim() || photo || manualText.trim())
  const snapshot = (): ReportDraft => ({
    id: draftId,
    ownerId: user.id,
    hazardTypeId,
    category,
    description,
    photo,
    // Without GPS the screen switches to typing the place by itself; the draft must remember that.
    manual: useManual,
    manualText,
    districtId,
    latitude: fix?.latitude ?? null,
    longitude: fix?.longitude ?? null,
    createdAt: startedAt,
    updatedAt: new Date().toISOString(),
  })
  const latest = useRef({ snapshot, loaded, hasContent })
  useEffect(() => {
    latest.current = { snapshot, loaded, hasContent }
  })

  // Everything typed is kept on this device, so leaving the screen by mistake loses nothing.
  useEffect(() => {
    if (!loaded || sent.current || !hasContent) return
    const timer = setTimeout(() => void saveDraft(latest.current.snapshot()), 500)
    return () => clearTimeout(timer)
  }, [loaded, hasContent, hazardTypeId, category, description, photo, useManual, manualText, districtId, fix])
  useEffect(
    () => () => {
      const { snapshot: take, loaded: ready, hasContent: filled } = latest.current
      if (ready && filled && !sent.current) void saveDraft(take())
    },
    [],
  )

  const errors = compact({
    hazardTypeId: hazardTypeId ? null : 'Choose what is happening.',
    category: category ? null : 'Choose what you see.',
    description: lengthError('Description', description, LIMITS.description.min, LIMITS.description.max),
    location: useManual
      ? manualText.trim().length >= 5
        ? null
        : 'Describe the place in at least 5 characters.'
      : fix
        ? null
        : 'Use your location or describe the place.',
    photo: photo && photo.size > LIMITS.photoBytes ? 'This photo is larger than 5 MB.' : null,
  })
  const show = (key: string) => (attempted ? (errors[key] ?? null) : null)

  /** Saves what was filled in first, then shows it for checking. */
  const review = async () => {
    setAttempted(true)
    if (Object.keys(errors).length > 0) return
    await saveDraft(snapshot())
    setStep('review')
  }

  const send = () => {
    const input: ReportInput = {
      clientRef: draftId,
      hazardTypeId: hazardTypeId!,
      category: category!,
      description: description.trim(),
      latitude: useManual ? null : fix!.latitude,
      longitude: useManual ? null : fix!.longitude,
      isManualLocation: useManual,
      manualLocationText: useManual ? manualText.trim() : null,
      districtId,
      capturedAt: startedAt,
      photo,
    }
    submit.mutate(input, {
      onSuccess: (result) => {
        sent.current = true
        void removeDraft(draftId)
        if (result.queued) navigate(paths.citizen.reportDone(result.clientRef), { state: { kind: 'queued' } })
        else
          navigate(paths.citizen.reportDone(result.report.id), {
            state: { kind: 'sent', referenceNo: result.report.referenceNo },
          })
      },
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
            value={useManual ? manualText.trim() : fix ? formatCoords(fix.latitude, fix.longitude) : undefined}
          />
          <Row label="District" value={ref.districtName(districtId)} />
          <Row label="Photo" value={photo ? undefined : 'None'}>
            {photo && <img src={URL.createObjectURL(photo)} alt="Photo attached to the report" className="mt-1 h-24 rounded-control object-cover" />}
          </Row>
        </dl>
        <div className="mt-4 space-y-3">
          <p className="text-sm text-muted">
            Saved on this device. If you leave now, you can send it later from{' '}
            <Link to={paths.citizen.reports} className="text-signal underline">
              My reports
            </Link>
            .
          </p>
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
        void review()
      }}
      className="space-y-5"
    >
      <PageHeader title="Report a hazard" subtitle="Tell the Disaster Management Centre what you see." />
      {otherDrafts.length > 0 && (
        <p className="rounded-control border border-signal/50 bg-panel px-3 py-2 text-sm text-ink">
          You have {otherDrafts.length === 1 ? 'a saved report' : `${otherDrafts.length} saved reports`} that{' '}
          {otherDrafts.length === 1 ? 'was' : 'were'} not sent.{' '}
          <Link to={paths.citizen.reports} className="text-signal underline">
            Open My reports
          </Link>
        </p>
      )}
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
          geo={geoView}
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
        Save and review
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
