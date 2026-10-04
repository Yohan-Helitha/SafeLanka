import type {
  Allocation,
  AllocationInput,
  Assignment,
  AssignmentInput,
  AssignmentStatus,
  DistributionInput,
  ReliefStock,
  RescueTeam,
  ResponseDashboard,
  Shelter,
  ShelterSuggestion,
  TeamStatusUpdate,
} from '@/types'

export interface ResponseApi {
  dashboard(districtId: string): Promise<ResponseDashboard>
  teams(filter?: { districtId?: string; available?: boolean }): Promise<RescueTeam[]>
  createAssignment(input: AssignmentInput): Promise<Assignment>
  assign(id: string, teamId: string): Promise<Assignment>
  cancelAssignment(id: string): Promise<Assignment>
  assignments(filter?: { districtId?: string; status?: AssignmentStatus }): Promise<Assignment[]>
  assignment(id: string): Promise<Assignment>
  /** The acting rescue member's live assignment, or null when there is none (HTTP 204). */
  myAssignment(): Promise<Assignment | null>
  respond(id: string, accept: boolean, declineReason?: string): Promise<Assignment>
  updateTeamStatus(teamId: string, update: TeamStatusUpdate): Promise<RescueTeam>
  shelters(filter?: { districtId?: string; coordinatorId?: string }): Promise<Shelter[]>
  shelterSuggestions(latitude: number, longitude: number): Promise<ShelterSuggestion[]>
  updateOccupancy(shelterId: string, occupancy: number): Promise<Shelter>
  stocks(filter?: { districtId?: string }): Promise<ReliefStock[]>
  allocate(input: AllocationInput): Promise<Allocation>
  recordDistribution(allocationId: string, input: DistributionInput): Promise<Allocation>
  allocations(filter?: { shelterId?: string; districtId?: string }): Promise<Allocation[]>
}
