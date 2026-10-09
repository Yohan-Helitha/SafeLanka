import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { POLL } from '@/constants/app'
import { useOutbox } from '@/context/OutboxContext'
import { api, isApiError } from '@/services'
import type { AllocationInput, AssignmentInput, DistributionInput, RescueTeam, TeamStatusUpdate } from '@/types'

export const responseKeys = {
  all: ['response'] as const,
  dashboard: (districtId: string) => ['response', 'dashboard', districtId] as const,
  teams: (districtId?: string) => ['response', 'teams', districtId ?? 'all'] as const,
  assignments: (districtId?: string) => ['response', 'assignments', districtId ?? 'all'] as const,
  mine: ['response', 'my-assignment'] as const,
  shelters: (filter: string) => ['response', 'shelters', filter] as const,
  headcountUpdates: (filter: string) => ['response', 'headcount-updates', filter] as const,
  suggestions: (lat: number, lng: number) => ['response', 'suggestions', lat, lng] as const,
  stocks: (districtId?: string) => ['response', 'stocks', districtId ?? 'all'] as const,
  allocations: (filter: string) => ['response', 'allocations', filter] as const,
}

export function useDashboard(districtId: string) {
  return useQuery({
    queryKey: responseKeys.dashboard(districtId),
    queryFn: () => api.response.dashboard(districtId),
    refetchInterval: POLL.situation,
  })
}

export function useTeams(districtId?: string) {
  return useQuery({ queryKey: responseKeys.teams(districtId), queryFn: () => api.response.teams({ districtId }) })
}

export function useAssignments(districtId?: string) {
  return useQuery({
    queryKey: responseKeys.assignments(districtId),
    queryFn: () => api.response.assignments({ districtId }),
    refetchInterval: POLL.situation,
  })
}

export function useMyAssignment() {
  return useQuery({
    queryKey: responseKeys.mine,
    queryFn: () => api.response.myAssignment(),
    refetchInterval: POLL.assignment,
  })
}

export function useShelters(filter: { districtId?: string; coordinatorId?: string }) {
  return useQuery({
    queryKey: responseKeys.shelters(JSON.stringify(filter)),
    queryFn: () => api.response.shelters(filter),
  })
}

export function useShelterSuggestions(lat: number | null, lng: number | null) {
  return useQuery({
    queryKey: responseKeys.suggestions(lat ?? 0, lng ?? 0),
    queryFn: () => api.response.shelterSuggestions(lat!, lng!),
    enabled: lat !== null && lng !== null,
  })
}

export function useStocks(districtId?: string) {
  return useQuery({ queryKey: responseKeys.stocks(districtId), queryFn: () => api.response.stocks({ districtId }) })
}

export function useAllocations(filter: { shelterId?: string; districtId?: string }) {
  return useQuery({
    queryKey: responseKeys.allocations(JSON.stringify(filter)),
    queryFn: () => api.response.allocations(filter),
  })
}

function useDone() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: responseKeys.all })
}

export function useCreateAssignment() {
  const done = useDone()
  return useMutation({ mutationFn: (input: AssignmentInput) => api.response.createAssignment(input), onSuccess: done })
}

export function useAssign() {
  const done = useDone()
  return useMutation({
    mutationFn: (v: { id: string; teamId: string }) => api.response.assign(v.id, v.teamId),
    onSuccess: done,
  })
}

export function useCancelAssignment() {
  const done = useDone()
  return useMutation({ mutationFn: (id: string) => api.response.cancelAssignment(id), onSuccess: done })
}

export function useRespond() {
  const done = useDone()
  return useMutation({
    mutationFn: (v: { id: string; accept: boolean; declineReason?: string }) =>
      api.response.respond(v.id, v.accept, v.declineReason),
    onSuccess: done,
  })
}

export type StatusResult = { queued: false; team: RescueTeam } | { queued: true }

/** Team status update that is saved on the phone when offline and sent later. */
export function useUpdateTeamStatus() {
  const done = useDone()
  const { enqueue } = useOutbox()
  return useMutation<StatusResult, Error, { teamId: string; update: TeamStatusUpdate; label: string }>({
    mutationFn: async ({ teamId, update, label }) => {
      try {
        return { queued: false, team: await api.response.updateTeamStatus(teamId, update) }
      } catch (e) {
        if (isApiError(e) && e.isOffline) {
          await enqueue(update.clientRef, { kind: 'TEAM_STATUS', teamId, update }, label)
          return { queued: true }
        }
        throw e
      }
    },
    onSuccess: done,
  })
}

export function useUpdateOccupancy() {
  const done = useDone()
  return useMutation({
    mutationFn: (v: { shelterId: string; occupancy: number }) => api.response.updateOccupancy(v.shelterId, v.occupancy),
    onSuccess: done,
  })
}

export function useHeadcountUpdates(filter?: { districtId?: string; status?: string }) {
  return useQuery({
    queryKey: responseKeys.headcountUpdates(JSON.stringify(filter ?? {})),
    queryFn: () => api.response.headcountUpdates(filter),
    refetchInterval: POLL.situation,
  })
}

export function useApplyHeadcountUpdate() {
  const done = useDone()
  return useMutation({
    mutationFn: (v: { updateId: string; customOccupancy?: number }) =>
      api.response.applyHeadcountUpdate(v.updateId, v.customOccupancy),
    onSuccess: done,
  })
}

export function useDismissHeadcountUpdate() {
  const done = useDone()
  return useMutation({
    mutationFn: (updateId: string) => api.response.dismissHeadcountUpdate(updateId),
    onSuccess: done,
  })
}

export function useCreateHeadcountUpdate() {
  const done = useDone()
  return useMutation({
    mutationFn: (input: import('@/types').CreateHeadcountUpdateInput) => api.response.createHeadcountUpdate(input),
    onSuccess: done,
  })
}


export function useAllocate() {
  const done = useDone()
  return useMutation({ mutationFn: (input: AllocationInput) => api.response.allocate(input), onSuccess: done })
}

export type DistributionResult = { queued: boolean }

export function useRecordDistribution() {
  const done = useDone()
  const { enqueue } = useOutbox()
  return useMutation<DistributionResult, Error, { allocationId: string; input: DistributionInput; label: string }>({
    mutationFn: async ({ allocationId, input, label }) => {
      try {
        await api.response.recordDistribution(allocationId, input)
        return { queued: false }
      } catch (e) {
        if (isApiError(e) && e.isOffline) {
          await enqueue(input.clientRef, { kind: 'DISTRIBUTION', allocationId, input }, label)
          return { queued: true }
        }
        throw e
      }
    },
    onSuccess: done,
  })
}
