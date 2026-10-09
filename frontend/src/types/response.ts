import type { Id } from './common'

export type TeamStatus = 'AVAILABLE' | 'DISPATCHED' | 'EN_ROUTE' | 'ACTIVE' | 'OFFLINE_UNKNOWN'
export type TeamType = 'BOAT' | 'MEDICAL' | 'SEARCH'
export type AssignmentStatus =
  | 'UNASSIGNED'
  | 'PENDING_ACK'
  | 'ACCEPTED'
  | 'EN_ROUTE'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'CANCELLED'
export type ShelterStatus = 'OPEN' | 'FULL' | 'CLOSED'
export type OccupancyLevel = 'OK' | 'AMBER' | 'FULL'

export interface RescueTeam {
  id: Id
  name: string
  organisationId: Id
  districtId: Id
  teamType: TeamType
  capacity: number
  status: TeamStatus
  lastStatusAt: string
}

export interface Assignment {
  id: Id
  eventId: Id
  warningId: Id | null
  teamId: Id | null
  teamName: string | null
  districtId: Id
  latitude: number
  longitude: number
  locationText: string
  task: string
  priority: 1 | 2 | 3
  peopleEstimated: number
  destinationShelterId: Id | null
  destinationShelterName: string | null
  status: AssignmentStatus
  declineReason: string | null
  assignedAt: string | null
  createdAt: string
}

export interface AssignmentInput {
  eventId: Id
  teamId: Id | null
  districtId: Id
  latitude: number
  longitude: number
  locationText: string
  task: string
  priority: 1 | 2 | 3
  peopleEstimated: number
  destinationShelterId: Id | null
}

export interface TeamStatusUpdate {
  toStatus: TeamStatus
  clientRef: Id
  changedAt: string
  recordedOffline: boolean
}

export interface Shelter {
  id: Id
  name: string
  districtId: Id
  address: string
  latitude: number
  longitude: number
  capacity: number
  currentOccupancy: number
  freeCapacity: number
  occupancyRatio: number
  level: OccupancyLevel
  status: ShelterStatus
  coordinatorId: Id | null
  addedBy?: string
  createdAt?: string
  updatedAt?: string
}


export interface ShelterSuggestion extends Shelter {
  distanceKm: number
}

export interface ReliefStock {
  id: Id
  itemId: Id
  itemName: string
  unit: string
  organisationId: Id
  organisationName: string
  districtId: Id
  quantityAvailable: number
}

export interface AllocationInput {
  stockId: Id
  shelterId: Id
  eventId: Id
  quantity: number
}

export interface Allocation {
  id: Id
  stockId: Id
  itemName: string
  unit: string
  organisationName: string
  shelterId: Id
  shelterName: string
  eventId: Id
  quantity: number
  distributed: number
  status: 'ALLOCATED' | 'PARTIALLY_DISTRIBUTED' | 'DISTRIBUTED' | 'CANCELLED'
  allocatedAt: string
}

export interface DistributionInput {
  quantityDistributed: number
  clientRef: Id
  distributedAt: string
  recordedOffline: boolean
}

export interface ActivityEntry {
  id: Id
  type: 'WARNING' | 'DISPATCH' | 'TEAM_STATUS' | 'SHELTER' | 'RELIEF'
  message: string
  occurredAt: string
}

export interface ResponseDashboard {
  districtId: Id
  activeEventId: Id | null
  teamsByStatus: Record<TeamStatus, number>
  openAssignments: number
  pendingAcknowledgement: number
  sheltersOccupied: number
  shelterCapacity: number
  nearlyFullShelters: number
  stockLines: number
  activeWarnings: number
  activity: ActivityEntry[]
}

export type HeadcountUpdateStatus = 'PENDING' | 'APPLIED' | 'DISMISSED'

export interface ShelterHeadcountUpdate {
  id: Id
  shelterId: Id
  shelterName: string
  districtId: Id
  reportedOccupancy: number
  previousOccupancy: number | null
  currentShelterOccupancy: number
  shelterCapacity: number
  reportedByName: string
  reportedByRole: string
  message: string
  status: HeadcountUpdateStatus
  reportedAt: string
  processedAt: string | null
}

export interface CreateHeadcountUpdateInput {
  shelterId: Id
  reportedOccupancy: number
  reportedByName?: string
  reportedByRole?: string
  message: string
}

