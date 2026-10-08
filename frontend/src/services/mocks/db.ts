import type {
  ActivityEntry,
  AppUser,
  Assignment,
  ChannelSetting,
  DisasterEvent,
  DisasterReport,
  District,
  HazardSource,
  HazardStatus,
  HazardType,
  Organisation,
  RejectionReason,
  ReliefItem,
  RescueTeam,
  RiverBasin,
  ReportStatus,
  Role,
  Sensor,
  ShelterStatus,
  Warning,
} from '@/types'
import type { Channel } from '@/types'
import { ID, newId, seedId } from './ids'

export interface ReportRow {
  id: string
  referenceNo: string
  reporterId: string
  hazardTypeId: string
  category: string
  description: string
  latitude: number | null
  longitude: number | null
  isManualLocation: boolean
  manualLocationText: string | null
  districtId: string
  status: ReportStatus
  clientRef: string
  capturedAt: string
  syncedAt: string
  photoUrl: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  rejectionReason: RejectionReason | null
  reviewComment: string | null
}

export interface HazardRow {
  id: string
  eventId: string | null
  hazardTypeId: string
  severity: number
  districtId: string | null
  riverBasinId: string | null
  description: string
  source: HazardSource
  sensorId: string | null
  status: HazardStatus
  detectedAt: string
  evidence: { reportId: string; linkedAt: string }[]
}

export interface DeliveryRow {
  id: string
  warningId: string
  citizenId: string
  districtId: string
  channel: Channel
  status: 'QUEUED' | 'DELIVERED' | 'FAILED'
  attemptedAt: string
  failureReason: string | null
}

export interface ShelterRow {
  id: string
  name: string
  districtId: string
  address: string
  latitude: number
  longitude: number
  capacity: number
  currentOccupancy: number
  status: ShelterStatus
  coordinatorId: string | null
}

export interface OccupancyLog {
  shelterId: string
  eventId: string
  occupancy: number
  delta: number
  recordedAt: string
}

export interface StockRow {
  id: string
  itemId: string
  organisationId: string
  districtId: string
  quantityAvailable: number
}

export interface AllocationRow {
  id: string
  stockId: string
  shelterId: string
  eventId: string
  quantity: number
  distributed: number
  status: 'ALLOCATED' | 'PARTIALLY_DISTRIBUTED' | 'DISTRIBUTED' | 'CANCELLED'
  allocatedBy: string
  allocatedAt: string
}

export interface DistributionRow {
  id: string
  allocationId: string
  quantity: number
  distributedAt: string
  clientRef: string | null
}

export interface ActivityRow extends ActivityEntry {
  districtId: string
}

const NOW = Date.now()
export const ago = (minutes: number): string => new Date(NOW - minutes * 60_000).toISOString()
const at = (isoWithOffset: string): string => new Date(isoWithOffset).toISOString()

// ---------- reference data ----------

const districts: District[] = [
  { id: ID.district.CMB, code: 'CMB', name: 'Colombo', province: 'Western' },
  { id: ID.district.GAM, code: 'GAM', name: 'Gampaha', province: 'Western' },
  { id: ID.district.KAL, code: 'KAL', name: 'Kalutara', province: 'Western' },
  { id: ID.district.RAT, code: 'RAT', name: 'Ratnapura', province: 'Sabaragamuwa' },
  { id: ID.district.KEG, code: 'KEG', name: 'Kegalle', province: 'Sabaragamuwa' },
]

const riverBasins: RiverBasin[] = [
  { id: ID.basin.KELANI, code: 'KELANI', name: 'Kelani Ganga', districtIds: [ID.district.CMB, ID.district.GAM, ID.district.KEG] },
  { id: ID.basin.KALU, code: 'KALU', name: 'Kalu Ganga', districtIds: [ID.district.RAT, ID.district.KAL] },
]

const hazardTypes: HazardType[] = [
  { id: ID.hazardType.FLOOD, code: 'FLOOD', name: 'Flood', onsetSpeed: 'RAPID', reportCategories: ['RISING_WATER', 'BLOCKED_ROAD', 'OTHER'], active: true },
  { id: ID.hazardType.LANDSLIDE, code: 'LANDSLIDE', name: 'Landslide', onsetSpeed: 'RAPID', reportCategories: ['LANDSLIDE_CRACK', 'BLOCKED_ROAD', 'OTHER'], active: true },
  { id: ID.hazardType.DROUGHT, code: 'DROUGHT', name: 'Drought', onsetSpeed: 'SLOW', reportCategories: ['WATER_SHORTAGE', 'OTHER'], active: false },
]

const organisations: Organisation[] = [
  { id: ID.org.DMC, name: 'Disaster Management Centre', type: 'GOVERNMENT' },
  { id: ID.org.MOH, name: 'Ministry of Health', type: 'GOVERNMENT' },
  { id: ID.org.ARMY, name: 'Sri Lanka Army', type: 'ARMED_FORCES' },
  { id: ID.org.NAVY, name: 'Sri Lanka Navy', type: 'ARMED_FORCES' },
  { id: ID.org.AIR_FORCE, name: 'Sri Lanka Air Force', type: 'ARMED_FORCES' },
  { id: ID.org.POLICE, name: 'Sri Lanka Police', type: 'POLICE' },
  { id: ID.org.RED_CROSS, name: 'Sri Lanka Red Cross Society', type: 'NGO' },
  { id: ID.org.SARVODAYA, name: 'Sarvodaya', type: 'NGO' },
  { id: ID.org.DONOR, name: 'Private Donor Network', type: 'PRIVATE_DONOR' },
]

const reliefItems: ReliefItem[] = [
  { id: ID.item.DRY_RATION, code: 'DRY_RATION', name: 'Dry ration pack', unit: 'packs', category: 'FOOD' },
  { id: ID.item.WATER, code: 'WATER_5L', name: 'Drinking water 5 L', unit: 'bottles', category: 'WATER' },
  { id: ID.item.FIRST_AID, code: 'FIRST_AID', name: 'First aid kit', unit: 'kits', category: 'MEDICINE' },
  { id: ID.item.HYGIENE, code: 'HYGIENE_KIT', name: 'Hygiene kit', unit: 'kits', category: 'HYGIENE' },
]

const events: DisasterEvent[] = [
  {
    id: ID.event.KELANI,
    name: 'Kelani Flood October 2026',
    hazardTypeId: ID.hazardType.FLOOD,
    status: 'ACTIVE',
    startedAt: at('2026-10-01T06:00:00+05:30'),
    endedAt: null,
    districtIds: [ID.district.CMB, ID.district.GAM],
  },
  {
    id: ID.event.KALU,
    name: 'Kalu Flood May 2026',
    hazardTypeId: ID.hazardType.FLOOD,
    status: 'CLOSED',
    startedAt: at('2026-05-14T04:00:00+05:30'),
    endedAt: at('2026-05-21T18:00:00+05:30'),
    districtIds: [ID.district.RAT, ID.district.KAL],
  },
]

function named(
  n: number,
  role: Role,
  fullName: string,
  districtId: string,
  basin: string | null,
  lang: 'en' | 'si' | 'ta',
  org: string | null = null,
  team: string | null = null,
): AppUser {
  return {
    id: seedId(6, n),
    role,
    fullName,
    districtId,
    riverBasinId: basin,
    organisationId: org,
    rescueTeamId: team,
    preferredLanguage: lang,
    named: true,
  }
}

const users: AppUser[] = [
  named(1, 'DMC_OFFICER', 'Nimal Perera', ID.district.CMB, null, 'en', ID.org.DMC),
  named(2, 'DISTRICT_OFFICER', 'Kasun Jayawardena', ID.district.CMB, null, 'en', ID.org.DMC),
  named(3, 'DISTRICT_OFFICER', 'Sanduni Wickramasinghe', ID.district.RAT, null, 'en', ID.org.DMC),
  named(4, 'CITIZEN', 'Ruwan Fernando', ID.district.CMB, ID.basin.KELANI, 'si'),
  named(5, 'VOLUNTEER', 'Tharindu Silva', ID.district.GAM, ID.basin.KELANI, 'si'),
  named(6, 'SHELTER_COORDINATOR', 'Dilani Gunasekara', ID.district.CMB, null, 'si', ID.org.DMC),
  named(7, 'RESCUE_MEMBER', 'Asanka Bandara', ID.district.CMB, null, 'si', ID.org.ARMY, ID.team(1)),
  named(8, 'RESCUE_MEMBER', 'Mahesh Kumara', ID.district.CMB, null, 'si', ID.org.NAVY, ID.team(2)),
  named(9, 'CITIZEN', 'Priya Shanmugam', ID.district.KAL, ID.basin.KALU, 'ta'),
]

// 40 demo residents across the five districts; every 4th is a volunteer.
const districtByIndex = [ID.district.CMB, ID.district.GAM, ID.district.KAL, ID.district.RAT, ID.district.KEG]
for (let n = 101; n <= 140; n++) {
  const districtId = districtByIndex[n % 5]
  const basin = riverBasins.find((b) => b.districtIds.includes(districtId))!.id
  users.push({
    id: seedId(6, n),
    role: n % 4 === 0 ? 'VOLUNTEER' : 'CITIZEN',
    fullName: `Demo Resident ${n}`,
    districtId,
    riverBasinId: basin,
    organisationId: null,
    rescueTeamId: null,
    preferredLanguage: (['si', 'ta', 'en'] as const)[(n + 1) % 3],
    named: false,
  })
}

// ---------- sensors ----------

function readings(base: number, slope: number, wobble: number) {
  return Array.from({ length: 24 }, (_, i) => ({
    recordedAt: ago((23 - i) * 60),
    value: Math.max(0, +(base + slope * i + Math.sin(i * 1.7) * wobble).toFixed(2)),
  }))
}

function sensor(
  n: number,
  code: string,
  name: string,
  basin: string,
  district: string,
  alert: number,
  major: number,
  series: ReturnType<typeof readings>,
): Sensor {
  return {
    id: ID.sensor(n),
    code,
    name,
    riverBasinId: basin,
    districtId: district,
    alertLevel: alert,
    majorFloodLevel: major,
    unit: 'm',
    latest: series[series.length - 1].value,
    readings: series,
  }
}

const sensors: Sensor[] = [
  sensor(1, 'KEL-NAGALAGAM', 'Kelani at Nagalagam Street', ID.basin.KELANI, ID.district.CMB, 1.2, 2.5, readings(0.62, 0.049, 0.02)),
  sensor(2, 'KEL-HANWELLA', 'Kelani at Hanwella', ID.basin.KELANI, ID.district.CMB, 7.0, 10.0, readings(5.1, 0.07, 0.1)),
  sensor(3, 'KEL-GLENCOURSE', 'Kelani at Glencourse', ID.basin.KELANI, ID.district.KEG, 16.0, 19.0, readings(13.4, 0.08, 0.15)),
  sensor(4, 'KALU-RATNAPURA', 'Kalu at Ratnapura', ID.basin.KALU, ID.district.RAT, 5.2, 7.5, readings(3.1, 0.01, 0.12)),
  sensor(5, 'KALU-ELLAGAWA', 'Kalu at Ellagawa', ID.basin.KALU, ID.district.RAT, 10.5, 13.0, readings(8.2, 0.0, 0.2)),
  sensor(6, 'KALU-PUTUPAULA', 'Kalu at Putupaula', ID.basin.KALU, ID.district.KAL, 2.5, 4.0, readings(1.4, 0.0, 0.1)),
]

// ---------- reports ----------

const PHOTO =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="400"><rect width="640" height="400" fill="#1E2942"/><path d="M0 280 Q80 250 160 280 T320 280 T480 280 T640 280 V400 H0Z" fill="#0E7490"/><path d="M0 320 Q80 290 160 320 T320 320 T480 320 T640 320 V400 H0Z" fill="#164E63"/><rect x="250" y="150" width="140" height="110" fill="#334155"/><path d="M240 150 L320 90 L400 150Z" fill="#475569"/></svg>',
  )

function report(
  n: number,
  reporter: number,
  hazard: 'FLOOD' | 'LANDSLIDE',
  category: string,
  district: keyof typeof ID.district,
  status: ReportStatus,
  lat: number,
  lng: number,
  capturedAt: string,
  description: string,
  extra: Partial<ReportRow> = {},
): ReportRow {
  const reviewed = status !== 'PENDING'
  return {
    id: seedId(12, n),
    referenceNo: `RPT-2026-${String(n).padStart(4, '0')}`,
    reporterId: seedId(6, reporter),
    hazardTypeId: ID.hazardType[hazard],
    category,
    description,
    latitude: lat,
    longitude: lng,
    isManualLocation: false,
    manualLocationText: null,
    districtId: ID.district[district],
    status,
    clientRef: seedId(13, n),
    capturedAt,
    syncedAt: new Date(new Date(capturedAt).getTime() + 3 * 60_000).toISOString(),
    photoUrl: null,
    reviewedBy: reviewed ? ID.user.DMC_OFFICER : null,
    reviewedAt: reviewed ? new Date(new Date(capturedAt).getTime() + 20 * 60_000).toISOString() : null,
    rejectionReason: null,
    reviewComment: null,
    ...extra,
  }
}

const reports: ReportRow[] = [
  report(1, 103, 'FLOOD', 'RISING_WATER', 'RAT', 'VERIFIED', 6.683, 80.4, at('2026-05-14T05:50:00+05:30'), 'Water entering houses along the Kalu river bank at Ratnapura town, rising fast.'),
  report(2, 4, 'FLOOD', 'RISING_WATER', 'CMB', 'VERIFIED', 6.933, 79.888, ago(1500), 'Water over the road near Kolonnawa station, ankle deep and rising.', { photoUrl: PHOTO }),
  report(3, 105, 'FLOOD', 'BLOCKED_ROAD', 'CMB', 'VERIFIED', 6.939, 79.892, ago(1380), 'Wellampitiya main road closed by flood water, buses are turning back.'),
  report(4, 104, 'LANDSLIDE', 'LANDSLIDE_CRACK', 'KEG', 'VERIFIED', 7.173, 80.459, ago(600), 'Wide crack across the slope above Aranayake road, three houses below.', { photoUrl: PHOTO }),
  report(5, 4, 'FLOOD', 'RISING_WATER', 'CMB', 'PENDING', 6.94, 79.889, ago(50), 'Canal overflowing at Sedawatte bridge, water reaching the shop fronts.', { photoUrl: PHOTO }),
  report(6, 105, 'FLOOD', 'RISING_WATER', 'CMB', 'PENDING', 6.9403, 79.8892, ago(35), 'Water is coming over Sedawatte bridge and children are being carried across.'),
  report(7, 5, 'FLOOD', 'RISING_WATER', 'GAM', 'PENDING', 6.9415, 79.9877, ago(90), 'Kelani water is in the fields behind Biyagama temple lane, still climbing.'),
  report(8, 9, 'FLOOD', 'BLOCKED_ROAD', 'KAL', 'PENDING', 6.585, 79.96, ago(25), 'Kalutara bridge road under water, only lorries can pass.'),
  report(9, 114, 'LANDSLIDE', 'LANDSLIDE_CRACK', 'KEG', 'PENDING', 7.25, 80.34, ago(15), 'Cracks in the tea estate road and a leaning pole above the line rooms.'),
  report(10, 110, 'FLOOD', 'OTHER', 'CMB', 'NEEDS_MORE_INFO', 6.945, 79.9, ago(200), 'Drain smell and water bubbling up inside the house.', {
    reviewComment: 'Can you add the street name and a photo?',
  }),
  report(11, 4, 'FLOOD', 'RISING_WATER', 'CMB', 'REJECTED', 6.93, 79.87, ago(3000), 'Water on the road after rain near the junction.', {
    rejectionReason: 'INSUFFICIENT_EVIDENCE',
    reviewComment: 'Not enough detail to confirm a flood.',
  }),
  report(12, 118, 'LANDSLIDE', 'LANDSLIDE_CRACK', 'RAT', 'REJECTED', 6.68, 80.39, ago(2000), 'Crack on the slope behind the school, same place as an earlier report.', {
    rejectionReason: 'DUPLICATE',
    reviewComment: 'Already covered by an earlier report.',
  }),
]

// ---------- hazards ----------

const hazards: HazardRow[] = [
  {
    id: seedId(14, 1),
    eventId: ID.event.KELANI,
    hazardTypeId: ID.hazardType.FLOOD,
    severity: 4,
    districtId: ID.district.CMB,
    riverBasinId: ID.basin.KELANI,
    description: 'Kelani river above alert level at Nagalagam Street and still rising.',
    source: 'SENSOR',
    sensorId: ID.sensor(1),
    status: 'WARNED',
    detectedAt: ago(26 * 60),
    evidence: [
      { reportId: seedId(12, 2), linkedAt: ago(1440) },
      { reportId: seedId(12, 3), linkedAt: ago(1370) },
    ],
  },
  {
    id: seedId(14, 2),
    eventId: null,
    hazardTypeId: ID.hazardType.LANDSLIDE,
    severity: 3,
    districtId: ID.district.KEG,
    riverBasinId: null,
    description: 'Ground cracks above the Aranayake road reported by residents.',
    source: 'REPORT',
    sensorId: null,
    status: 'UNDER_ASSESSMENT',
    detectedAt: ago(580),
    evidence: [{ reportId: seedId(12, 4), linkedAt: ago(580) }],
  },
  {
    id: seedId(14, 3),
    eventId: ID.event.KELANI,
    hazardTypeId: ID.hazardType.FLOOD,
    severity: 2,
    districtId: ID.district.GAM,
    riverBasinId: null,
    description: 'Low-lying paddy and roads near Biyagama flooding in patches.',
    source: 'MANUAL',
    sensorId: null,
    status: 'MONITORING',
    detectedAt: ago(8 * 60),
    evidence: [],
  },
  {
    id: seedId(14, 4),
    eventId: ID.event.KALU,
    hazardTypeId: ID.hazardType.FLOOD,
    severity: 4,
    districtId: null,
    riverBasinId: ID.basin.KALU,
    description: 'Kalu river overtopping banks at Ratnapura and Putupaula.',
    source: 'REPORT',
    sensorId: null,
    status: 'RESOLVED',
    detectedAt: at('2026-05-14T06:30:00+05:30'),
    evidence: [{ reportId: seedId(12, 1), linkedAt: at('2026-05-14T06:30:00+05:30') }],
  },
]

// ---------- warnings and deliveries ----------

const channelSettings: ChannelSetting[] = [
  { channel: 'PUSH', enabled: true, simulateFailure: false },
  { channel: 'SMS', enabled: true, simulateFailure: false },
  { channel: 'AUDIBLE', enabled: true, simulateFailure: false },
]

export function districtsOfBasins(basinIds: string[]): string[] {
  return riverBasins.filter((b) => basinIds.includes(b.id)).flatMap((b) => b.districtIds)
}

export function recipientsFor(districtIds: string[], basinIds: string[]): AppUser[] {
  return users.filter(
    (u) =>
      (u.role === 'CITIZEN' || u.role === 'VOLUNTEER') &&
      (districtIds.includes(u.districtId) || (u.riverBasinId !== null && basinIds.includes(u.riverBasinId))),
  )
}

/** Generates per-recipient delivery rows. ~8% of SMS fail in seed data; live runs obey the channel switches. */
export function makeDeliveries(
  warning: Pick<Warning, 'id' | 'level' | 'issuedAt'>,
  recipients: AppUser[],
  opts: { seeded: boolean },
): DeliveryRow[] {
  const rows: DeliveryRow[] = []
  recipients.forEach((u, i) => {
    const channels: Channel[] = ['PUSH', 'SMS']
    if (warning.level === 'WARNING' || warning.level === 'EVACUATE') channels.push('AUDIBLE')
    channels.forEach((channel) => {
      const setting = channelSettings.find((c) => c.channel === channel)!
      if (!opts.seeded && !setting.enabled) return
      const failed = opts.seeded ? channel === 'SMS' && i % 12 === 0 : setting.simulateFailure
      rows.push({
        id: newId(),
        warningId: warning.id,
        citizenId: u.id,
        districtId: u.districtId,
        channel,
        status: failed ? 'FAILED' : 'DELIVERED',
        attemptedAt: new Date(new Date(warning.issuedAt).getTime() + (i + 1) * 4000).toISOString(),
        failureReason: failed
          ? channel === 'SMS' && opts.seeded
            ? 'Number unreachable'
            : 'Gateway unavailable (simulated)'
          : null,
      })
    })
  })
  return rows
}

export function summariseDeliveries(rows: DeliveryRow[]): Warning['deliverySummary'] {
  const byChannel = {
    PUSH: { delivered: 0, failed: 0 },
    SMS: { delivered: 0, failed: 0 },
    AUDIBLE: { delivered: 0, failed: 0 },
  } as Warning['deliverySummary']['byChannel']
  rows.forEach((r) => {
    if (r.status === 'DELIVERED') byChannel[r.channel].delivered += 1
    if (r.status === 'FAILED') byChannel[r.channel].failed += 1
  })
  const delivered = Object.values(byChannel).reduce((s, c) => s + c.delivered, 0)
  const failed = Object.values(byChannel).reduce((s, c) => s + c.failed, 0)
  return { targeted: new Set(rows.map((r) => r.citizenId)).size, delivered, failed, byChannel }
}

const warnings: Warning[] = []
const deliveries: DeliveryRow[] = []

type SeedWarning = Omit<
  Warning,
  'deliverySummary' | 'reached' | 'districtIds' | 'levelChangedAt' | 'levelHistory'
>

function seedWarning(w: SeedWarning) {
  const districtIds = districtsOfBasins(w.riverBasinIds)
  const rows = makeDeliveries(w, recipientsFor([], w.riverBasinIds), { seeded: true })
  const summary = summariseDeliveries(rows)
  deliveries.push(...rows)
  warnings.push({
    ...w,
    districtIds,
    levelChangedAt: w.issuedAt,
    levelHistory: [{ from: null, to: w.level, changedBy: w.issuedBy, changedAt: w.issuedAt }],
    reached: summary.targeted,
    deliverySummary: summary,
  })
}

const kaluIds = [seedId(15, 2), seedId(15, 3), seedId(15, 4)]
const kaluCommon = {
  hazardId: seedId(14, 4),
  eventId: ID.event.KALU,
  targetType: 'RIVER_BASIN' as const,
  riverBasinIds: [ID.basin.KALU],
  issuedBy: ID.user.DMC_OFFICER,
  cancelledAt: null,
  cancelReason: null,
  reportIds: [seedId(12, 1)],
}

seedWarning({
  id: seedId(15, 1),
  hazardId: seedId(14, 1),
  eventId: ID.event.KELANI,
  level: 'WARNING',
  status: 'ACTIVE',
  targetType: 'RIVER_BASIN',
  riverBasinIds: [ID.basin.KELANI],
  title: 'Kelani river flood warning',
  message:
    'River levels at Nagalagam Street are above the alert level and still rising. Low-lying areas along the Kelani river in Colombo, Gampaha and Kegalle may flood in the next 6 hours.',
  smsText: 'DMC WARNING: Kelani river rising. Move valuables up and be ready to leave. Dial 117 for help.',
  instructions: 'Move to higher ground if water enters your street. Keep documents and medicine ready.',
  issuedBy: ID.user.DMC_OFFICER,
  issuedAt: ago(300),
  cancelledAt: null,
  cancelReason: null,
  reportIds: [seedId(12, 2), seedId(12, 3)],
})
seedWarning({
  ...kaluCommon,
  id: kaluIds[0],
  level: 'WATCH',
  status: 'ESCALATED',
  title: 'Kalu river watch',
  message: 'The Kalu river is rising at Ratnapura. Be ready to act if levels keep climbing.',
  smsText: 'DMC WATCH: Kalu river rising at Ratnapura. Be ready to act. Dial 117 for help.',
  instructions: 'Check on neighbours and move valuables off the floor.',
  issuedAt: at('2026-05-14T07:35:00+05:30'),
})
seedWarning({
  ...kaluCommon,
  id: kaluIds[1],
  level: 'WARNING',
  status: 'ESCALATED',
  title: 'Kalu river flood warning',
  message: 'The Kalu river has passed its alert level. Low-lying areas in Ratnapura and Kalutara will flood.',
  smsText: 'DMC WARNING: Kalu river flooding. Prepare to move to higher ground. Dial 117.',
  instructions: 'Pack essentials and be ready to leave on short notice.',
  issuedAt: at('2026-05-14T12:10:00+05:30'),
})
seedWarning({
  ...kaluCommon,
  id: kaluIds[2],
  level: 'EVACUATE',
  status: 'EXPIRED',
  title: 'Kalu river evacuation',
  message: 'Major flood level reached. Leave low-lying homes in Ratnapura and Kalutara now.',
  smsText: 'DMC EVACUATE: Kalu river at major flood level. Leave now for the nearest shelter. Dial 117.',
  instructions: 'Leave now for the nearest open shelter. Take documents and medicine.',
  issuedAt: at('2026-05-15T02:30:00+05:30'),
})

// ---------- response: teams, assignments, shelters, stock ----------

const teams: RescueTeam[] = [
  { id: ID.team(1), name: 'Army Rapid Response Unit - Colombo', organisationId: ID.org.ARMY, districtId: ID.district.CMB, teamType: 'SEARCH', capacity: 12, status: 'AVAILABLE', lastStatusAt: ago(240) },
  { id: ID.team(2), name: 'Navy Boat Team - Kelani A', organisationId: ID.org.NAVY, districtId: ID.district.CMB, teamType: 'BOAT', capacity: 8, status: 'DISPATCHED', lastStatusAt: ago(30) },
  { id: ID.team(3), name: 'SLAF Rescue Wing - Katunayake', organisationId: ID.org.AIR_FORCE, districtId: ID.district.GAM, teamType: 'SEARCH', capacity: 10, status: 'AVAILABLE', lastStatusAt: ago(300) },
  { id: ID.team(4), name: 'Police Life Saving Unit - Gampaha', organisationId: ID.org.POLICE, districtId: ID.district.GAM, teamType: 'BOAT', capacity: 6, status: 'AVAILABLE', lastStatusAt: ago(300) },
  { id: ID.team(5), name: 'Red Cross Medical Team - Colombo', organisationId: ID.org.RED_CROSS, districtId: ID.district.CMB, teamType: 'MEDICAL', capacity: 6, status: 'AVAILABLE', lastStatusAt: ago(300) },
  { id: ID.team(6), name: 'DMC Volunteer Response - Ratnapura', organisationId: ID.org.DMC, districtId: ID.district.RAT, teamType: 'SEARCH', capacity: 15, status: 'AVAILABLE', lastStatusAt: ago(300) },
]

const assignments: Assignment[] = [
  {
    id: seedId(16, 1),
    eventId: ID.event.KELANI,
    warningId: seedId(15, 1),
    teamId: ID.team(2),
    teamName: teams[1].name,
    districtId: ID.district.CMB,
    latitude: 6.9335,
    longitude: 79.8885,
    locationText: 'Station Road, Kolonnawa',
    task: 'Bring 12 people out of flooded houses on Station Road and take them to the shelter.',
    priority: 1,
    peopleEstimated: 12,
    destinationShelterId: ID.shelter(1),
    destinationShelterName: 'Kolonnawa Maha Vidyalaya',
    status: 'PENDING_ACK',
    declineReason: null,
    assignedAt: ago(30),
    createdAt: ago(35),
  },
  {
    id: seedId(16, 2),
    eventId: ID.event.KELANI,
    warningId: null,
    teamId: null,
    teamName: null,
    districtId: ID.district.CMB,
    latitude: 6.944,
    longitude: 79.878,
    locationText: 'Temple Road, Dematagoda',
    task: 'Five people waiting on a rooftop behind the temple. Boat needed.',
    priority: 2,
    peopleEstimated: 5,
    destinationShelterId: ID.shelter(1),
    destinationShelterName: 'Kolonnawa Maha Vidyalaya',
    status: 'UNASSIGNED',
    declineReason: null,
    assignedAt: null,
    createdAt: ago(20),
  },
  {
    id: seedId(16, 3),
    eventId: ID.event.KELANI,
    warningId: seedId(15, 1),
    teamId: ID.team(1),
    teamName: teams[0].name,
    districtId: ID.district.CMB,
    latitude: 6.939,
    longitude: 79.892,
    locationText: 'Wellampitiya',
    task: 'Move elderly residents from the flooded care home to the community hall.',
    priority: 2,
    peopleEstimated: 9,
    destinationShelterId: ID.shelter(2),
    destinationShelterName: 'Wellampitiya Community Hall',
    status: 'COMPLETED',
    declineReason: null,
    assignedAt: ago(600),
    createdAt: ago(620),
  },
]

const shelters: ShelterRow[] = [
  { id: ID.shelter(1), name: 'Kolonnawa Maha Vidyalaya', districtId: ID.district.CMB, address: 'Kolonnawa', latitude: 6.933, longitude: 79.888, capacity: 300, currentOccupancy: 140, status: 'OPEN', coordinatorId: null },
  { id: ID.shelter(2), name: 'Wellampitiya Community Hall', districtId: ID.district.CMB, address: 'Wellampitiya', latitude: 6.939, longitude: 79.892, capacity: 200, currentOccupancy: 185, status: 'OPEN', coordinatorId: ID.user.COORDINATOR },
  { id: ID.shelter(3), name: 'Kaduwela Bodhirajaramaya', districtId: ID.district.CMB, address: 'Kaduwela', latitude: 6.933, longitude: 79.984, capacity: 120, currentOccupancy: 120, status: 'FULL', coordinatorId: null },
  { id: ID.shelter(4), name: 'Biyagama Central College', districtId: ID.district.GAM, address: 'Biyagama', latitude: 6.9415, longitude: 79.9877, capacity: 250, currentOccupancy: 40, status: 'OPEN', coordinatorId: null },
  { id: ID.shelter(5), name: 'Kelaniya Community Centre', districtId: ID.district.GAM, address: 'Kelaniya', latitude: 6.9553, longitude: 79.922, capacity: 150, currentOccupancy: 0, status: 'CLOSED', coordinatorId: null },
  { id: ID.shelter(6), name: 'Ratnapura Sivali Central College', districtId: ID.district.RAT, address: 'Ratnapura', latitude: 6.683, longitude: 80.4, capacity: 300, currentOccupancy: 0, status: 'OPEN', coordinatorId: null },
]

const occupancyLogs: OccupancyLog[] = [
  { shelterId: ID.shelter(1), eventId: ID.event.KELANI, occupancy: 140, delta: 140, recordedAt: at('2026-10-02T09:00:00+05:30') },
  { shelterId: ID.shelter(2), eventId: ID.event.KELANI, occupancy: 185, delta: 185, recordedAt: at('2026-10-02T09:30:00+05:30') },
  { shelterId: ID.shelter(3), eventId: ID.event.KELANI, occupancy: 120, delta: 120, recordedAt: at('2026-10-02T10:00:00+05:30') },
  { shelterId: ID.shelter(4), eventId: ID.event.KELANI, occupancy: 40, delta: 40, recordedAt: at('2026-10-02T10:30:00+05:30') },
]

// Closed Kalu flood: five days of headcounts at the Ratnapura shelter, peaking at 290 of 300.
{
  const profile = [30, 120, 210, 270, 290, 260, 200, 150, 90, 40]
  let previous = 0
  profile.forEach((occupancy, i) => {
    occupancyLogs.push({
      shelterId: ID.shelter(6),
      eventId: ID.event.KALU,
      occupancy,
      delta: occupancy - previous,
      recordedAt: new Date(Date.parse('2026-05-14T18:00:00+05:30') + i * 12 * 3_600_000).toISOString(),
    })
    previous = occupancy
  })
}

const stocks: StockRow[] = [
  [1, ID.item.DRY_RATION, ID.org.DMC, ID.district.CMB, 500],
  [2, ID.item.DRY_RATION, ID.org.RED_CROSS, ID.district.CMB, 300],
  [3, ID.item.DRY_RATION, ID.org.DONOR, ID.district.GAM, 200],
  [4, ID.item.WATER, ID.org.ARMY, ID.district.CMB, 800],
  [5, ID.item.WATER, ID.org.SARVODAYA, ID.district.GAM, 400],
  [6, ID.item.FIRST_AID, ID.org.MOH, ID.district.CMB, 120],
  [7, ID.item.FIRST_AID, ID.org.RED_CROSS, ID.district.GAM, 60],
  [8, ID.item.HYGIENE, ID.org.SARVODAYA, ID.district.CMB, 150],
  [9, ID.item.HYGIENE, ID.org.NAVY, ID.district.CMB, 100],
  [10, ID.item.DRY_RATION, ID.org.DMC, ID.district.RAT, 400],
  [11, ID.item.WATER, ID.org.AIR_FORCE, ID.district.RAT, 300],
].map(([n, itemId, organisationId, districtId, quantityAvailable]) => ({
  id: ID.stock(n as number),
  itemId: itemId as string,
  organisationId: organisationId as string,
  districtId: districtId as string,
  quantityAvailable: quantityAvailable as number,
}))

const allocations: AllocationRow[] = [
  { id: seedId(17, 1), stockId: ID.stock(4), shelterId: ID.shelter(2), eventId: ID.event.KELANI, quantity: 200, distributed: 120, status: 'PARTIALLY_DISTRIBUTED', allocatedBy: ID.user.DISTRICT_COLOMBO, allocatedAt: ago(900) },
  { id: seedId(17, 2), stockId: ID.stock(1), shelterId: ID.shelter(2), eventId: ID.event.KELANI, quantity: 150, distributed: 0, status: 'ALLOCATED', allocatedBy: ID.user.DISTRICT_COLOMBO, allocatedAt: ago(500) },
  { id: seedId(17, 3), stockId: ID.stock(10), shelterId: ID.shelter(6), eventId: ID.event.KALU, quantity: 200, distributed: 200, status: 'DISTRIBUTED', allocatedBy: ID.user.DISTRICT_RATNAPURA, allocatedAt: at('2026-05-15T08:00:00+05:30') },
  { id: seedId(17, 4), stockId: ID.stock(11), shelterId: ID.shelter(6), eventId: ID.event.KALU, quantity: 150, distributed: 150, status: 'DISTRIBUTED', allocatedBy: ID.user.DISTRICT_RATNAPURA, allocatedAt: at('2026-05-15T09:00:00+05:30') },
]

const distributions: DistributionRow[] = [
  { id: newId(), allocationId: seedId(17, 1), quantity: 120, distributedAt: ago(400), clientRef: null },
  { id: newId(), allocationId: seedId(17, 3), quantity: 200, distributedAt: at('2026-05-16T10:00:00+05:30'), clientRef: null },
  { id: newId(), allocationId: seedId(17, 4), quantity: 150, distributedAt: at('2026-05-16T11:00:00+05:30'), clientRef: null },
]

const activity: ActivityRow[] = [
  { id: newId(), districtId: ID.district.CMB, type: 'WARNING', message: 'Warning issued: Kelani river flood warning', occurredAt: ago(300) },
  { id: newId(), districtId: ID.district.CMB, type: 'DISPATCH', message: 'Navy Boat Team - Kelani A dispatched to Station Road, Kolonnawa', occurredAt: ago(30) },
  { id: newId(), districtId: ID.district.CMB, type: 'SHELTER', message: 'Wellampitiya Community Hall at 185 of 200', occurredAt: ago(120) },
  { id: newId(), districtId: ID.district.CMB, type: 'RELIEF', message: '120 bottles of drinking water handed out at Wellampitiya', occurredAt: ago(400) },
  { id: newId(), districtId: ID.district.CMB, type: 'TEAM_STATUS', message: 'Army Rapid Response Unit - Colombo completed an assignment', occurredAt: ago(480) },
  { id: newId(), districtId: ID.district.GAM, type: 'WARNING', message: 'Warning issued: Kelani river flood warning', occurredAt: ago(300) },
  { id: newId(), districtId: ID.district.GAM, type: 'SHELTER', message: 'Biyagama Central College at 40 of 250', occurredAt: ago(240) },
  { id: newId(), districtId: ID.district.KEG, type: 'WARNING', message: 'Warning issued: Kelani river flood warning', occurredAt: ago(300) },
  { id: newId(), districtId: ID.district.RAT, type: 'RELIEF', message: 'Relief stock checked: 400 dry ration packs available', occurredAt: ago(700) },
]

export const db = {
  districts,
  riverBasins,
  hazardTypes,
  organisations,
  reliefItems,
  events,
  users,
  sensors,
  reports,
  hazards,
  warnings,
  deliveries,
  channelSettings,
  teams,
  assignments,
  shelters,
  occupancyLogs,
  stocks,
  allocations,
  distributions,
  activity,
  analyticsReports: [] as DisasterReport[],
  nextReportNo: reports.length + 1,
}

export function userById(id: string | null): AppUser | undefined {
  return users.find((u) => u.id === id)
}

export function districtName(id: string | null): string {
  return districts.find((d) => d.id === id)?.name ?? '—'
}
