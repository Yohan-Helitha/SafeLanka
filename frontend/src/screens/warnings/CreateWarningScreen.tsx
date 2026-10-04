import { Users } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiErrorNotice, SeverityBadge } from '@/components/domain'
import { Button, Card, Checkbox, Field, PageHeader, Segmented, Select } from '@/components/ui'
import { AlertPreview } from '@/components/warnings/AlertPreview'
import { ChannelChips } from '@/components/warnings/ChannelChips'
import { textErrors, WarningTextFields } from '@/components/warnings/WarningTextFields'
import { LEVEL_ORDER } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useToast } from '@/context/ToastContext'
import { isApiError } from '@/services'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import {
  useAudience,
  useChannelSettings,
  useHazard,
  useHazards,
  usePublishWarning,
} from '@/hooks/warnings/useWarnings'
import { SEVERITY } from '@/theme/tokens'
import type { TargetType, WarningLevel, WarningUpdate } from '@/types'
import { formatNumber, plural } from '@/utils/format'

const toggle = (list: string[], id: string) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id])
const levelForSeverity = (s: number): WarningLevel => (s >= 5 ? 'EVACUATE' : s === 4 ? 'WARNING' : s === 3 ? 'WATCH' : 'ADVISORY')

export function CreateWarningScreen() {
  useDocumentTitle('New warning')
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const ref = useReferenceData()
  const hazards = useHazards(false)
  const channels = useChannelSettings()
  const publish = usePublishWarning()

  const [step, setStep] = useState<'compose' | 'review'>('compose')
  const [attempted, setAttempted] = useState(false)
  const [hazardId, setHazardId] = useState(params.get('hazardId') ?? '')
  const [level, setLevel] = useState<WarningLevel | null>(null)
  const [targetType, setTargetType] = useState<TargetType>('DISTRICT')
  const [districtIds, setDistrictIds] = useState<string[]>([])
  const [basinIds, setBasinIds] = useState<string[]>([])
  const [text, setText] = useState<WarningUpdate>({ title: '', message: '', smsText: '', instructions: '' })
  const [reportIds, setReportIds] = useState<string[]>([])

  const hazard = useHazard(hazardId)
  const prefilled = useRef('')
  // Choosing a hazard pre-fills its area, evidence and a suggested level.
  useEffect(() => {
    const h = hazard.data
    if (!h || prefilled.current === h.id) return
    prefilled.current = h.id
    if (h.riverBasinId) {
      setTargetType('RIVER_BASIN')
      setBasinIds([h.riverBasinId])
      setDistrictIds([])
    } else if (h.districtId) {
      setTargetType('DISTRICT')
      setDistrictIds([h.districtId])
      setBasinIds([])
    }
    setReportIds(h.evidence.map((e) => e.reportId))
    setLevel((current) => current ?? levelForSeverity(h.severity))
  }, [hazard.data])

  const areaDistricts = targetType === 'DISTRICT' ? districtIds : []
  const areaBasins = targetType === 'RIVER_BASIN' ? basinIds : []
  const audience = useAudience(areaDistricts, areaBasins)

  const errors = textErrors(text)
  const problems: string[] = [
    ...(hazardId ? [] : ['Choose a hazard.']),
    ...(level ? [] : ['Choose a warning level.']),
    ...(areaDistricts.length + areaBasins.length ? [] : ['Choose at least one area to warn.']),
    ...(Object.values(errors).some(Boolean) ? ['Complete the message fields.'] : []),
  ]

  const coveredDistricts =
    targetType === 'RIVER_BASIN'
      ? ref.riverBasins.filter((b) => basinIds.includes(b.id)).flatMap((b) => b.districtIds)
      : districtIds

  const review = () => {
    setAttempted(true)
    if (problems.length === 0) setStep('review')
  }

  const send = () => {
    publish.mutate(
      {
        hazardId,
        level: level!,
        targetType,
        districtIds: areaDistricts,
        riverBasinIds: areaBasins,
        title: text.title.trim(),
        message: text.message.trim(),
        smsText: text.smsText.trim(),
        instructions: text.instructions.trim(),
        reportIds,
        confirm: true,
      },
      {
        onSuccess: (w) => {
          toast(`Warning broadcast to ${plural(w.reached, 'person', 'people')}`)
          navigate(paths.dmc.warning(w.id))
        },
      },
    )
  }

  if (step === 'review' && level) {
    const conflictId = isApiError(publish.error) && publish.error.code === 'CONFLICT' ? (publish.error.details?.warningId as string | undefined) : undefined
    return (
      <div>
        <PageHeader
          title="Review before broadcasting"
          subtitle="Once sent, the warning reaches phones immediately and cannot be recalled. You can still escalate or cancel it later."
        />
        <div className="mb-4 grid gap-3 sm:grid-cols-3">
          <Tile label="Level">
            <SeverityBadge level={level} size="lg" />
          </Tile>
          <Tile label="Areas">
            <p className="text-sm text-ink">
              {(targetType === 'RIVER_BASIN' ? basinIds.map(ref.basinName) : districtIds.map(ref.districtName)).join(', ')}
            </p>
            <p className="mt-1 text-xs text-muted">Districts covered: {coveredDistricts.map(ref.districtName).join(', ')}</p>
          </Tile>
          <Tile label="Recipients">
            <p className="tabular font-display text-[40px] font-semibold leading-none text-signal">{formatNumber(audience.data?.recipients ?? 0)}</p>
          </Tile>
        </div>
        <div className="space-y-4">
          <Card title="Channels">
            <ChannelChips settings={channels.data ?? []} level={level} />
          </Card>
          <Card title="Preview">
            <AlertPreview level={level} title={text.title} instructions={text.instructions} smsText={text.smsText} />
          </Card>
          <ApiErrorNotice error={publish.error}>
            {conflictId && (
              <Link className="mt-1 inline-block text-signal hover:underline" to={paths.dmc.warning(conflictId)}>
                Open the active warning to escalate or update it
              </Link>
            )}
          </ApiErrorNotice>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setStep('compose')}>
              Edit
            </Button>
            <Button size="lg" variant={level === 'EVACUATE' ? 'danger' : 'primary'} loading={publish.isPending} onClick={send}>
              Confirm and broadcast
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
    >
      <PageHeader backTo={paths.dmc.warnings} backLabel="Warnings" title="New warning" subtitle="Write the warning, choose who receives it, then review before it goes out." />
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="space-y-4">
          <Card title="Hazard and level">
            <div className="space-y-4">
              <Field label="Hazard" required error={attempted && !hazardId ? 'Choose a hazard.' : null}>
                {(p) => (
                  <Select {...p} value={hazardId} onChange={(e) => setHazardId(e.target.value)}>
                    <option value="">Choose an open hazard</option>
                    {(hazards.data ?? []).map((h) => (
                      <option key={h.id} value={h.id}>
                        {ref.hazardTypeName(h.hazardTypeId)} · {ref.areaName(h.districtId, h.riverBasinId)} · severity {h.severity}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
              <Segmented
                legend="Warning level"
                columns={2}
                value={level}
                onChange={setLevel}
                error={attempted && !level ? 'Choose a warning level.' : null}
                options={LEVEL_ORDER.map((l) => ({ value: l, label: <SeverityBadge level={l} size="sm" />, description: SEVERITY[l].action }))}
              />
            </div>
          </Card>

          <Card title="Who to warn">
            <div className="space-y-3">
              <Segmented<TargetType>
                legend="Warn by"
                hideLegend
                value={targetType}
                onChange={setTargetType}
                options={[
                  { value: 'DISTRICT', label: 'Districts' },
                  { value: 'RIVER_BASIN', label: 'River basins', description: 'Everyone along the river' },
                ]}
              />
              <div className="grid gap-x-4 sm:grid-cols-2">
                {targetType === 'DISTRICT'
                  ? ref.districts.map((d) => (
                      <Checkbox key={d.id} label={d.name} checked={districtIds.includes(d.id)} onChange={() => setDistrictIds((l) => toggle(l, d.id))} />
                    ))
                  : ref.riverBasins.map((b) => (
                      <Checkbox
                        key={b.id}
                        label={b.name}
                        description={b.districtIds.map(ref.districtName).join(', ')}
                        checked={basinIds.includes(b.id)}
                        onChange={() => setBasinIds((l) => toggle(l, b.id))}
                      />
                    ))}
              </div>
              {attempted && areaDistricts.length + areaBasins.length === 0 && <p className="text-sm text-danger">Choose at least one area to warn.</p>}
            </div>
          </Card>

          <Card title="Message">
            <WarningTextFields value={text} onChange={setText} errors={attempted ? errors : {}} />
          </Card>
        </div>

        <aside className="space-y-4 lg:sticky lg:top-28 lg:self-start">
          <Card title="Recipients">
            <div className="flex items-center gap-3">
              <Users className="size-6 text-signal" aria-hidden />
              <p className="tabular font-display text-[36px] font-semibold leading-none text-ink">
                {areaDistricts.length + areaBasins.length ? formatNumber(audience.data?.recipients ?? 0) : '—'}
              </p>
            </div>
            <p className="mt-1 text-sm text-muted">{areaDistricts.length + areaBasins.length ? 'people will be warned' : 'Choose an area to see how many people it reaches.'}</p>
          </Card>
          <Card title="Evidence">
            {hazard.data && hazard.data.evidence.length > 0 ? (
              <div>
                {hazard.data.evidence.map((e) => (
                  <Checkbox
                    key={e.reportId}
                    label={<span className="tabular">{e.referenceNo}</span>}
                    description={e.description}
                    checked={reportIds.includes(e.reportId)}
                    onChange={() => setReportIds((l) => toggle(l, e.reportId))}
                  />
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted">No verified reports are linked to this hazard.</p>
            )}
          </Card>
          <ApiErrorNotice error={attempted && problems.length ? new Error(problems[0]) : null} />
          <Button type="submit" size="lg" block>
            Review warning
          </Button>
        </aside>
      </div>
    </form>
  )
}

function Tile({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-card border border-line bg-panel p-4">
      <p className="mb-2 text-sm text-muted">{label}</p>
      {children}
    </div>
  )
}
