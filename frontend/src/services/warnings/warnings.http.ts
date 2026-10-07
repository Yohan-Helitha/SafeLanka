import { http } from '../http'
import type { SensorTick, WarningsApi } from './warningsApi'
import {
  toCitizenAlert,
  toDelivery,
  toDeliverySummary,
  toHazardDetail,
  toHazardListItem,
  toPage,
  toSimulationSensor,
  toWarning,
  toWarningListItem,
} from './warnings.wire'
import type {
  WireAlert,
  WireAudience,
  WireDelivery,
  WireDeliverySummary,
  WireHazardDetail,
  WireHazardListItem,
  WirePage,
  WireSensor,
  WireTick,
  WireWarning,
} from './warnings.wire'

/** The list shows open hazards by default; "include resolved" asks for every status. */
const ALL_HAZARD_STATUSES = 'UNDER_ASSESSMENT,WARNED,MONITORING,RESOLVED'

/** The warnings list is a table without paging controls, so it asks for the largest page. */
const WARNING_LIST_SIZE = 100

/** After a simulated reading the screen needs the gauge, so it is read back from the list. */
async function sensorAfter(sensorId: string, tick: WireTick): Promise<SensorTick> {
  const sensors = await http.get<WireSensor[]>('/simulation/sensors')
  const sensor = sensors.find((s) => s.id === sensorId)
  if (!sensor) throw new Error(`Sensor ${sensorId} is not in the simulation list`)
  return { sensor: toSimulationSensor(sensor), newHazardId: tick.hazardId }
}

export const warningsHttp: WarningsApi = {
  hazards: async (filter) =>
    (
      await http.get<WireHazardListItem[]>('/hazards', {
        status: filter?.includeResolved ? ALL_HAZARD_STATUSES : undefined,
      })
    ).map(toHazardListItem),
  hazard: async (id) => toHazardDetail(await http.get<WireHazardDetail>(`/hazards/${id}`)),
  createHazard: async (input) => toHazardDetail(await http.post<WireHazardDetail>('/hazards', input)),
  setHazardStatus: async (id, status) =>
    toHazardDetail(await http.patch<WireHazardDetail>(`/hazards/${id}/status`, { status })),

  audience: async (districtIds, basinIds) => {
    // The API needs at least one area; nothing chosen simply reaches nobody.
    if (districtIds.length === 0 && basinIds.length === 0) return { recipients: 0 }
    const audience = await http.get<WireAudience>('/warnings/audience', {
      districtIds,
      riverBasinIds: basinIds,
    })
    return { recipients: audience.citizenCount }
  },

  publish: async (input) =>
    toWarning(
      await http.post<WireWarning>('/warnings', {
        hazardId: input.hazardId,
        level: input.level,
        targetType: input.targetType,
        // A warning targets districts or river basins, never both.
        districtIds: input.targetType === 'DISTRICT' ? input.districtIds : [],
        riverBasinIds: input.targetType === 'RIVER_BASIN' ? input.riverBasinIds : [],
        title: input.title,
        message: input.message,
        smsText: input.smsText,
        instructions: input.instructions,
        evidenceReportIds: input.reportIds,
        confirm: input.confirm,
      }),
    ),
  update: async (id, input) => toWarning(await http.put<WireWarning>(`/warnings/${id}`, input)),
  // The escalate dialog is itself the review step, so the request is already confirmed.
  escalate: async (id, input) =>
    toWarning(await http.post<WireWarning>(`/warnings/${id}/escalate`, { ...input, confirm: true })),
  cancel: async (id, reason) => toWarning(await http.post<WireWarning>(`/warnings/${id}/cancel`, { reason })),

  list: async (status) =>
    (
      await http.get<WirePage<WireWarning>>('/warnings', {
        status: status && status !== 'ALL' ? status : undefined,
        size: WARNING_LIST_SIZE,
      })
    ).content.map(toWarningListItem),
  get: async (id) => toWarning(await http.get<WireWarning>(`/warnings/${id}`)),

  deliveries: async (id, filter) => {
    const result = await http.get<{ summary: WireDeliverySummary; items: WirePage<WireDelivery> }>(
      `/warnings/${id}/deliveries`,
      {
        status: filter.status === 'ALL' ? undefined : filter.status,
        channel: filter.channel === 'ALL' ? undefined : filter.channel,
        page: filter.page,
        size: filter.size,
      },
    )
    return { summary: toDeliverySummary(result.summary), items: toPage(result.items, toDelivery) }
  },
  myAlerts: async () => (await http.get<WireAlert[]>('/warnings/active/mine')).map(toCitizenAlert),

  simulation: {
    sensors: async () => (await http.get<WireSensor[]>('/simulation/sensors')).map(toSimulationSensor),
    tick: async (id) => sensorAfter(id, await http.post<WireTick>(`/simulation/sensors/${id}/tick`)),
    setReading: async (id, value) =>
      sensorAfter(id, await http.post<WireTick>(`/simulation/sensors/${id}/readings`, { value })),
    channels: () => http.get('/simulation/channels'),
    setChannel: (channel, patch) => http.patch(`/simulation/channels/${channel}`, patch),
  },
}
