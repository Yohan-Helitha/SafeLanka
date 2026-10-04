import { THRESHOLDS } from '@/theme/tokens'
import type {
  ActivityEntry,
  Allocation,
  Assignment,
  AssignmentStatus,
  RescueTeam,
  ResponseDashboard,
  Shelter,
  TeamStatus,
} from '@/types'
import { haversineMetres } from '@/utils/geo'
import { actor, requireRole } from '../mocks/actor'
import { bus } from '../mocks/bus'
import { db, districtName } from '../mocks/db'
import type { ActivityRow, AllocationRow, ShelterRow } from '../mocks/db'
import { newId } from '../mocks/ids'
import { fail, mockCall, notFound, validationFail } from '../mocks/mockCall'
import type { ResponseApi } from './responseApi'

const LIVE: AssignmentStatus[] = ['PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE']

const TEAM_TRANSITIONS: Record<TeamStatus, TeamStatus[]> = {
  AVAILABLE: ['DISPATCHED'],
  DISPATCHED: ['EN_ROUTE'],
  EN_ROUTE: ['ACTIVE'],
  ACTIVE: ['AVAILABLE'],
  OFFLINE_UNKNOWN: ['AVAILABLE'],
}

const seenClientRefs = new Set<string>()

function log(districtId: string, type: ActivityRow['type'], message: string) {
  db.activity.push({ id: newId(), districtId, type, message, occurredAt: new Date().toISOString() })
}

function shelterView(s: ShelterRow): Shelter {
  const ratio = s.capacity ? s.currentOccupancy / s.capacity : 0
  const level = s.status === 'FULL' || s.currentOccupancy >= s.capacity ? 'FULL' : ratio >= THRESHOLDS.shelterAmber ? 'AMBER' : 'OK'
  return {
    ...s,
    freeCapacity: s.capacity - s.currentOccupancy,
    occupancyRatio: ratio,
    level,
  }
}

function allocationView(a: AllocationRow): Allocation {
  const stock = db.stocks.find((s) => s.id === a.stockId)!
  const item = db.reliefItems.find((i) => i.id === stock.itemId)!
  return {
    id: a.id,
    stockId: a.stockId,
    itemName: item.name,
    unit: item.unit,
    organisationName: db.organisations.find((o) => o.id === stock.organisationId)!.name,
    shelterId: a.shelterId,
    shelterName: db.shelters.find((s) => s.id === a.shelterId)!.name,
    eventId: a.eventId,
    quantity: a.quantity,
    distributed: a.distributed,
    status: a.status,
    allocatedAt: a.allocatedAt,
  }
}

function assignmentRow(id: string): Assignment {
  return db.assignments.find((a) => a.id === id) ?? notFound('Assignment')
}

function teamRow(id: string): RescueTeam {
  return db.teams.find((t) => t.id === id) ?? notFound('Rescue team')
}

function dispatch(a: Assignment, teamId: string) {
  const team = teamRow(teamId)
  if (team.status !== 'AVAILABLE') {
    const alternatives = db.teams
      .filter((t) => t.status === 'AVAILABLE' && t.districtId === team.districtId && t.id !== team.id)
      .map((t) => ({ id: t.id, name: t.name }))
    fail('TEAM_NOT_AVAILABLE', `${team.name} is not available right now.`, { alternatives })
  }
  team.status = 'DISPATCHED'
  team.lastStatusAt = new Date().toISOString()
  a.teamId = team.id
  a.teamName = team.name
  a.status = 'PENDING_ACK'
  a.assignedAt = new Date().toISOString()
  a.declineReason = null
  log(a.districtId, 'DISPATCH', `${team.name} dispatched to ${a.locationText}`)
}

// Warning events add rows to the district activity feed, as the response module does on the backend.
bus.subscribe((event) => {
  if (event.type === 'ReportVerified') return
  const w = db.warnings.find((x) => x.id === event.warningId)
  if (!w) return
  const verb =
    event.type === 'WarningPublished' ? 'issued' : event.type === 'WarningEscalated' ? 'escalated' : 'cancelled'
  w.districtIds.forEach((d) => log(d, 'WARNING', `Warning ${verb}: ${w.title}`))
})

export const responseMock: ResponseApi = {
  dashboard: (districtId) =>
    mockCall<ResponseDashboard>(() => {
      requireRole('DISTRICT_OFFICER', 'DMC_OFFICER')
      const teamsByStatus: Record<TeamStatus, number> = {
        AVAILABLE: 0,
        DISPATCHED: 0,
        EN_ROUTE: 0,
        ACTIVE: 0,
        OFFLINE_UNKNOWN: 0,
      }
      db.teams.filter((t) => t.districtId === districtId).forEach((t) => (teamsByStatus[t.status] += 1))
      const mine = db.assignments.filter((a) => a.districtId === districtId)
      const sheltersInDistrict = db.shelters.filter((s) => s.districtId === districtId).map(shelterView)
      const entries: ActivityEntry[] = db.activity
        .filter((a) => a.districtId === districtId)
        .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))
        .slice(0, 8)
        .map(({ districtId: _d, ...rest }) => rest)
      return {
        districtId,
        activeEventId: db.events.find((e) => e.status === 'ACTIVE' && e.districtIds.includes(districtId))?.id ?? null,
        teamsByStatus,
        openAssignments: mine.filter((a) => LIVE.includes(a.status) || a.status === 'UNASSIGNED').length,
        pendingAcknowledgement: mine.filter((a) => a.status === 'PENDING_ACK').length,
        sheltersOccupied: sheltersInDistrict.reduce((n, s) => n + s.currentOccupancy, 0),
        shelterCapacity: sheltersInDistrict.reduce((n, s) => n + s.capacity, 0),
        nearlyFullShelters: sheltersInDistrict.filter((s) => s.level !== 'OK').length,
        stockLines: db.stocks.filter((s) => s.districtId === districtId).length,
        activeWarnings: db.warnings.filter((w) => w.status === 'ACTIVE' && w.districtIds.includes(districtId)).length,
        activity: entries,
      }
    }),

  teams: (filter) =>
    mockCall(() => {
      actor()
      return db.teams.filter(
        (t) => (!filter?.districtId || t.districtId === filter.districtId) && (!filter?.available || t.status === 'AVAILABLE'),
      )
    }),

  createAssignment: (input) =>
    mockCall(() => {
      const user = requireRole('DISTRICT_OFFICER', 'DMC_OFFICER')
      const errors: Record<string, string> = {}
      if (input.task.trim().length < 5 || input.task.trim().length > 500) errors.task = 'Describe the task in 5–500 characters.'
      if (input.locationText.trim().length < 3) errors.locationText = 'Add a place name.'
      if (!Number.isFinite(input.latitude) || !Number.isFinite(input.longitude)) errors.location = 'Add coordinates for the location.'
      if (input.peopleEstimated < 0) errors.peopleEstimated = 'People cannot be negative.'
      if (Object.keys(errors).length) validationFail(errors)
      const shelter = input.destinationShelterId ? db.shelters.find((s) => s.id === input.destinationShelterId) : null
      const a: Assignment = {
        id: newId(),
        eventId: input.eventId,
        warningId: null,
        teamId: null,
        teamName: null,
        districtId: input.districtId || user.districtId,
        latitude: input.latitude,
        longitude: input.longitude,
        locationText: input.locationText.trim(),
        task: input.task.trim(),
        priority: input.priority,
        peopleEstimated: input.peopleEstimated,
        destinationShelterId: shelter?.id ?? null,
        destinationShelterName: shelter?.name ?? null,
        status: 'UNASSIGNED',
        declineReason: null,
        assignedAt: null,
        createdAt: new Date().toISOString(),
      }
      if (input.teamId) dispatch(a, input.teamId) // throws TEAM_NOT_AVAILABLE before anything is saved
      db.assignments.push(a)
      return a
    }),

  assign: (id, teamId) =>
    mockCall(() => {
      requireRole('DISTRICT_OFFICER', 'DMC_OFFICER')
      const a = assignmentRow(id)
      if (a.status !== 'UNASSIGNED') fail('INVALID_STATE_TRANSITION', 'This assignment already has a team.')
      dispatch(a, teamId)
      return a
    }),

  cancelAssignment: (id) =>
    mockCall(() => {
      requireRole('DISTRICT_OFFICER', 'DMC_OFFICER')
      const a = assignmentRow(id)
      if (a.status === 'COMPLETED' || a.status === 'CANCELLED') {
        fail('INVALID_STATE_TRANSITION', 'This assignment is already closed.')
      }
      if (a.teamId) {
        const team = teamRow(a.teamId)
        team.status = 'AVAILABLE'
        team.lastStatusAt = new Date().toISOString()
      }
      a.status = 'CANCELLED'
      return a
    }),

  assignments: (filter) =>
    mockCall(() => {
      actor()
      return db.assignments
        .filter((a) => (!filter?.districtId || a.districtId === filter.districtId) && (!filter?.status || a.status === filter.status))
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    }),

  assignment: (id) => mockCall(() => assignmentRow(id)),

  myAssignment: () =>
    mockCall(() => {
      const user = requireRole('RESCUE_MEMBER')
      return db.assignments.find((a) => a.teamId === user.rescueTeamId && LIVE.includes(a.status)) ?? null
    }),

  respond: (id, accept, declineReason) =>
    mockCall(() => {
      const user = requireRole('RESCUE_MEMBER')
      const a = assignmentRow(id)
      if (a.teamId !== user.rescueTeamId) fail('FORBIDDEN_ROLE', 'This assignment belongs to another team.')
      if (a.status !== 'PENDING_ACK') fail('INVALID_STATE_TRANSITION', 'This assignment was already answered.')
      if (accept) {
        a.status = 'ACCEPTED'
        return a
      }
      if ((declineReason?.trim().length ?? 0) < 5) validationFail({ declineReason: 'Give a reason of at least 5 characters.' })
      const team = teamRow(a.teamId!)
      team.status = 'AVAILABLE'
      team.lastStatusAt = new Date().toISOString()
      log(a.districtId, 'TEAM_STATUS', `${team.name} declined an assignment at ${a.locationText}`)
      a.status = 'UNASSIGNED'
      a.teamId = null
      a.teamName = null
      a.declineReason = declineReason!.trim()
      return a
    }),

  updateTeamStatus: (teamId, update) =>
    mockCall(() => {
      const user = actor()
      if (user.role === 'RESCUE_MEMBER' && user.rescueTeamId !== teamId) fail('FORBIDDEN_ROLE', 'This is not your team.')
      const team = teamRow(teamId)
      if (seenClientRefs.has(update.clientRef)) return team // idempotent retry
      if (!TEAM_TRANSITIONS[team.status].includes(update.toStatus)) {
        fail('INVALID_STATE_TRANSITION', `A team cannot go from ${team.status} to ${update.toStatus}.`)
      }
      seenClientRefs.add(update.clientRef)
      const live = db.assignments.find((a) => a.teamId === teamId && LIVE.includes(a.status))
      if (live) {
        if (update.toStatus === 'EN_ROUTE') live.status = 'EN_ROUTE'
        if (update.toStatus === 'ACTIVE') live.status = 'ACTIVE'
        if (update.toStatus === 'AVAILABLE') live.status = 'COMPLETED'
      }
      team.status = update.toStatus
      team.lastStatusAt = update.changedAt
      log(team.districtId, 'TEAM_STATUS', `${team.name} is now ${update.toStatus.toLowerCase().replace(/_/g, ' ')}`)
      return team
    }),

  shelters: (filter) =>
    mockCall(() => {
      actor()
      return db.shelters
        .filter((s) => (!filter?.districtId || s.districtId === filter.districtId) && (!filter?.coordinatorId || s.coordinatorId === filter.coordinatorId))
        .map(shelterView)
    }),

  shelterSuggestions: (latitude, longitude) =>
    mockCall(() => {
      actor()
      return db.shelters
        .filter((s) => s.status === 'OPEN' && s.currentOccupancy < s.capacity)
        .map((s) => ({
          ...shelterView(s),
          distanceKm: +(haversineMetres({ latitude, longitude }, s) / 1000).toFixed(1),
        }))
        .sort((a, b) => a.distanceKm - b.distanceKm)
        .slice(0, 5)
    }),

  updateOccupancy: (id, occupancy) =>
    mockCall(() => {
      const user = requireRole('SHELTER_COORDINATOR', 'DISTRICT_OFFICER')
      const s = db.shelters.find((x) => x.id === id) ?? notFound('Shelter')
      if (user.role === 'SHELTER_COORDINATOR' && s.coordinatorId !== user.id) fail('FORBIDDEN_ROLE', 'This is not your shelter.')
      if (!Number.isInteger(occupancy) || occupancy < 0) validationFail({ occupancy: 'Enter a headcount of 0 or more.' })
      if (occupancy > s.capacity) {
        fail('CAPACITY_EXCEEDED', `${s.name} holds ${s.capacity} people.`, { capacity: s.capacity })
      }
      db.occupancyLogs.push({
        shelterId: s.id,
        eventId: db.events.find((e) => e.status === 'ACTIVE')?.id ?? db.events[0].id,
        occupancy,
        delta: occupancy - s.currentOccupancy,
        recordedAt: new Date().toISOString(),
      })
      s.currentOccupancy = occupancy
      if (s.status !== 'CLOSED') s.status = occupancy >= s.capacity ? 'FULL' : 'OPEN'
      log(s.districtId, 'SHELTER', `${s.name} at ${occupancy} of ${s.capacity}`)
      return shelterView(s)
    }),

  stocks: (filter) =>
    mockCall(() => {
      actor()
      return db.stocks
        .filter((s) => !filter?.districtId || s.districtId === filter.districtId)
        .map((s) => {
          const item = db.reliefItems.find((i) => i.id === s.itemId)!
          return {
            id: s.id,
            itemId: s.itemId,
            itemName: item.name,
            unit: item.unit,
            organisationId: s.organisationId,
            organisationName: db.organisations.find((o) => o.id === s.organisationId)!.name,
            districtId: s.districtId,
            quantityAvailable: s.quantityAvailable,
          }
        })
    }),

  allocate: (input) =>
    mockCall(() => {
      const user = requireRole('DISTRICT_OFFICER')
      const stock = db.stocks.find((s) => s.id === input.stockId) ?? notFound('Stock line')
      const shelter = db.shelters.find((s) => s.id === input.shelterId) ?? notFound('Shelter')
      if (!Number.isInteger(input.quantity) || input.quantity <= 0) validationFail({ quantity: 'Enter a quantity above 0.' })
      if (shelter.status === 'CLOSED') fail('BUSINESS_RULE', `${shelter.name} is closed.`)
      if (input.quantity > stock.quantityAvailable) {
        const alternatives = db.stocks
          .filter((s) => s.itemId === stock.itemId && s.id !== stock.id && s.quantityAvailable >= input.quantity)
          .map((s) => ({
            stockId: s.id,
            organisationName: db.organisations.find((o) => o.id === s.organisationId)!.name,
            districtName: districtName(s.districtId),
            quantityAvailable: s.quantityAvailable,
          }))
        fail('INSUFFICIENT_STOCK', 'There is not enough stock for this allocation.', {
          available: stock.quantityAvailable,
          shortfall: input.quantity - stock.quantityAvailable,
          alternatives,
        })
      }
      stock.quantityAvailable -= input.quantity
      const row: AllocationRow = {
        id: newId(),
        stockId: stock.id,
        shelterId: shelter.id,
        eventId: input.eventId,
        quantity: input.quantity,
        distributed: 0,
        status: 'ALLOCATED',
        allocatedBy: user.id,
        allocatedAt: new Date().toISOString(),
      }
      db.allocations.push(row)
      const view = allocationView(row)
      log(shelter.districtId, 'RELIEF', `${input.quantity} ${view.unit} of ${view.itemName.toLowerCase()} allocated to ${shelter.name}`)
      return view
    }),

  recordDistribution: (allocationId, input) =>
    mockCall(() => {
      const user = requireRole('SHELTER_COORDINATOR', 'DISTRICT_OFFICER')
      const a = db.allocations.find((x) => x.id === allocationId) ?? notFound('Allocation')
      const shelter = db.shelters.find((s) => s.id === a.shelterId)!
      if (user.role === 'SHELTER_COORDINATOR' && shelter.coordinatorId !== user.id) fail('FORBIDDEN_ROLE', 'This is not your shelter.')
      if (db.distributions.some((d) => d.clientRef === input.clientRef)) return allocationView(a) // idempotent retry
      const remaining = a.quantity - a.distributed
      if (!Number.isInteger(input.quantityDistributed) || input.quantityDistributed <= 0) {
        validationFail({ quantityDistributed: 'Enter a quantity above 0.' })
      }
      if (input.quantityDistributed > remaining) {
        fail('BUSINESS_RULE', `Only ${remaining} left to hand out from this allocation.`, { remaining })
      }
      a.distributed += input.quantityDistributed
      a.status = a.distributed >= a.quantity ? 'DISTRIBUTED' : 'PARTIALLY_DISTRIBUTED'
      db.distributions.push({
        id: newId(),
        allocationId,
        quantity: input.quantityDistributed,
        distributedAt: input.distributedAt,
        clientRef: input.clientRef,
      })
      const view = allocationView(a)
      log(shelter.districtId, 'RELIEF', `${input.quantityDistributed} ${view.unit} of ${view.itemName.toLowerCase()} handed out at ${shelter.name}`)
      return view
    }),

  allocations: (filter) =>
    mockCall(() => {
      actor()
      return db.allocations
        .filter((a) => !filter?.shelterId || a.shelterId === filter.shelterId)
        .filter((a) => !filter?.districtId || db.shelters.find((s) => s.id === a.shelterId)?.districtId === filter.districtId)
        .sort((a, b) => b.allocatedAt.localeCompare(a.allocatedAt))
        .map(allocationView)
    }),
}
