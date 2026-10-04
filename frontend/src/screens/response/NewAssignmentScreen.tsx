import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiErrorNotice } from '@/components/domain'
import { Button, Card, Field, Input, LocationMap, PageHeader, Segmented, Select, TextArea } from '@/components/ui'
import { PRIORITY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { isApiError } from '@/services'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useCreateAssignment, useShelterSuggestions, useTeams } from '@/hooks/response/useResponse'
import { compact } from '@/utils/validation'

interface Alt {
  id: string
  name: string
}

export function NewAssignmentScreen() {
  useDocumentTitle('New assignment')
  const user = useCurrentUser()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { events } = useReferenceData()
  const teams = useTeams(user.districtId)
  const create = useCreateAssignment()

  const [attempted, setAttempted] = useState(false)
  const [locationText, setLocationText] = useState('')
  const [lat, setLat] = useState('6.9335')
  const [lng, setLng] = useState('79.8885')
  const [task, setTask] = useState('')
  const [priority, setPriority] = useState<1 | 2 | 3>(2)
  const [people, setPeople] = useState('0')
  const [shelterId, setShelterId] = useState('')
  const [teamId, setTeamId] = useState('')

  const latN = Number(lat)
  const lngN = Number(lng)
  const coordsOk = lat !== '' && lng !== '' && Number.isFinite(latN) && Number.isFinite(lngN)
  const suggestions = useShelterSuggestions(coordsOk ? latN : null, coordsOk ? lngN : null)
  const event = events.find((e) => e.status === 'ACTIVE' && e.districtIds.includes(user.districtId)) ?? events.find((e) => e.status === 'ACTIVE')

  const errors = compact({
    locationText: locationText.trim().length >= 3 ? null : 'Add a place name.',
    location: coordsOk ? null : 'Enter latitude and longitude.',
    task: task.trim().length >= 5 && task.trim().length <= 500 ? null : 'Describe the task in 5–500 characters.',
    people: Number.isInteger(Number(people)) && Number(people) >= 0 ? null : 'Enter a number of people, 0 or more.',
    event: event ? null : 'There is no active event to attach this assignment to.',
  })
  const show = (k: string) => (attempted ? (errors[k] ?? null) : null)
  const alternatives =
    isApiError(create.error) && create.error.code === 'TEAM_NOT_AVAILABLE' ? ((create.error.details?.alternatives as Alt[] | undefined) ?? []) : []
  const available = (teams.data ?? []).filter((t) => t.status === 'AVAILABLE')

  const submit = () => {
    setAttempted(true)
    if (Object.keys(errors).length || !event) return
    create.mutate(
      {
        eventId: event.id,
        teamId: teamId || null,
        districtId: user.districtId,
        latitude: latN,
        longitude: lngN,
        locationText: locationText.trim(),
        task: task.trim(),
        priority,
        peopleEstimated: Number(people),
        destinationShelterId: shelterId || null,
      },
      {
        onSuccess: () => {
          toast(teamId ? 'Team dispatched' : 'Assignment created')
          navigate(paths.district.teams)
        },
      },
    )
  }

  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        submit()
      }}
      className="max-w-3xl"
    >
      <PageHeader backTo={paths.district.teams} backLabel="Rescue teams" title="New assignment" subtitle="Describe the task, then dispatch a team now or assign one later." />
      <div className="space-y-4">
        <Card title="Where">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field className="sm:col-span-2" label="Place" required error={show('locationText')}>
              {(p) => <Input {...p} value={locationText} onChange={(e) => setLocationText(e.target.value)} placeholder="Station Road, Kolonnawa" />}
            </Field>
            <div className="sm:col-span-2">
              <LocationMap
                height={240}
                label="Map: click to set the assignment location"
                onPick={(la, lo) => {
                  setLat(la.toFixed(5))
                  setLng(lo.toFixed(5))
                }}
                markers={[
                  ...(coordsOk ? [{ id: 'here', latitude: latN, longitude: lngN, label: 'Assignment location', tone: 'danger' as const }] : []),
                  ...(suggestions.data ?? []).map((s) => ({ id: s.id, latitude: s.latitude, longitude: s.longitude, label: s.name, tone: 'ok' as const })),
                ]}
              />
              <p className="mt-1 text-xs text-muted">Click the map to set the location. Green dots are shelters with space.</p>
            </div>
            <Field label="Latitude" required error={show('location')}>
              {(p) => <Input {...p} inputMode="decimal" value={lat} onChange={(e) => setLat(e.target.value)} />}
            </Field>
            <Field label="Longitude" required>
              {(p) => <Input {...p} inputMode="decimal" value={lng} onChange={(e) => setLng(e.target.value)} />}
            </Field>
          </div>
        </Card>
        <Card title="Task">
          <div className="space-y-4">
            <Field label="What needs to be done" required error={show('task')}>
              {(p) => <TextArea {...p} rows={3} value={task} onChange={(e) => setTask(e.target.value)} placeholder="Bring 12 people out of flooded houses and take them to the shelter." />}
            </Field>
            <Segmented<1 | 2 | 3>
              legend="Priority"
              columns={3}
              value={priority}
              onChange={setPriority}
              options={([1, 2, 3] as const).map((p) => ({ value: p, label: PRIORITY_LABEL[p] }))}
            />
            <Field label="People to help (estimate)" error={show('people')}>
              {(p) => <Input {...p} type="number" inputMode="numeric" min={0} value={people} onChange={(e) => setPeople(e.target.value)} />}
            </Field>
            <Field label="Take them to" hint="Nearest open shelters with space, closest first.">
              {(p) => (
                <Select {...p} value={shelterId} onChange={(e) => setShelterId(e.target.value)}>
                  <option value="">No destination yet</option>
                  {(suggestions.data ?? []).map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} · {s.distanceKm} km · {s.freeCapacity} places free
                    </option>
                  ))}
                </Select>
              )}
            </Field>
          </div>
        </Card>
        <Card title="Team">
          <Field label="Dispatch now" hint="Leave empty to assign a team later from Rescue teams.">
            {(p) => (
              <Select {...p} value={teamId} onChange={(e) => setTeamId(e.target.value)}>
                <option value="">Assign later</option>
                {available.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        </Card>
        <ApiErrorNotice error={create.error ?? (attempted && errors.event ? new Error(errors.event) : null)}>
          {alternatives.length > 0 && (
            <ul className="mt-1 space-y-1">
              {alternatives.map((a) => (
                <li key={a.id}>
                  <button type="button" className="text-signal hover:underline" onClick={() => { setTeamId(a.id); create.reset() }}>
                    Use {a.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </ApiErrorNotice>
        <div className="flex justify-end">
          <Button type="submit" size="lg" loading={create.isPending}>
            {teamId ? 'Create and dispatch' : 'Create assignment'}
          </Button>
        </div>
      </div>
    </form>
  )
}
