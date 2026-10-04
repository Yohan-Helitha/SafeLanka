import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { POLL } from '@/constants/app'
import { api } from '@/services'
import type {
  Channel,
  ChannelSetting,
  DeliveryFilter,
  EscalateInput,
  HazardInput,
  HazardStatus,
  WarningInput,
  WarningStatus,
  WarningUpdate,
} from '@/types'

export const warningKeys = {
  all: ['warnings'] as const,
  hazards: (includeResolved: boolean) => ['warnings', 'hazards', includeResolved] as const,
  hazard: (id: string) => ['warnings', 'hazard', id] as const,
  audience: (districts: string[], basins: string[]) => ['warnings', 'audience', districts, basins] as const,
  list: (status: string) => ['warnings', 'list', status] as const,
  detail: (id: string) => ['warnings', 'detail', id] as const,
  deliveries: (id: string, filter: DeliveryFilter) => ['warnings', 'deliveries', id, filter] as const,
  alerts: ['warnings', 'alerts'] as const,
  active: ['warnings', 'active'] as const,
  sensors: ['warnings', 'sensors'] as const,
  channels: ['warnings', 'channels'] as const,
}

export function useHazards(includeResolved: boolean) {
  return useQuery({
    queryKey: warningKeys.hazards(includeResolved),
    queryFn: () => api.warnings.hazards({ includeResolved }),
  })
}

export function useHazard(id: string) {
  return useQuery({ queryKey: warningKeys.hazard(id), queryFn: () => api.warnings.hazard(id), enabled: Boolean(id) })
}

export function useCreateHazard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: HazardInput) => api.warnings.createHazard(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: warningKeys.all }),
  })
}

export function useSetHazardStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (v: { id: string; status: HazardStatus }) => api.warnings.setHazardStatus(v.id, v.status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: warningKeys.all }),
  })
}

export function useAudience(districtIds: string[], basinIds: string[]) {
  return useQuery({
    queryKey: warningKeys.audience(districtIds, basinIds),
    queryFn: () => api.warnings.audience(districtIds, basinIds),
    enabled: districtIds.length + basinIds.length > 0,
  })
}

export function useWarningsList(status: WarningStatus | 'ALL') {
  return useQuery({ queryKey: warningKeys.list(status), queryFn: () => api.warnings.list(status) })
}

/** Active warnings for the situation strip; refreshed every 30 s. */
export function useActiveWarnings() {
  return useQuery({
    queryKey: warningKeys.active,
    queryFn: () => api.warnings.list('ACTIVE'),
    refetchInterval: POLL.situation,
  })
}

export function useWarning(id: string) {
  return useQuery({ queryKey: warningKeys.detail(id), queryFn: () => api.warnings.get(id) })
}

export function useDeliveries(id: string, filter: DeliveryFilter) {
  return useQuery({
    queryKey: warningKeys.deliveries(id, filter),
    queryFn: () => api.warnings.deliveries(id, filter),
    placeholderData: (previous) => previous,
  })
}

export function useMyAlerts() {
  return useQuery({
    queryKey: warningKeys.alerts,
    queryFn: () => api.warnings.myAlerts(),
    refetchInterval: POLL.alerts,
  })
}

function useWarningMutations() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: warningKeys.all })
}

export function usePublishWarning() {
  const done = useWarningMutations()
  return useMutation({ mutationFn: (input: WarningInput) => api.warnings.publish(input), onSuccess: done })
}

export function useUpdateWarning() {
  const done = useWarningMutations()
  return useMutation({
    mutationFn: (v: { id: string; input: WarningUpdate }) => api.warnings.update(v.id, v.input),
    onSuccess: done,
  })
}

export function useEscalateWarning() {
  const done = useWarningMutations()
  return useMutation({
    mutationFn: (v: { id: string; input: EscalateInput }) => api.warnings.escalate(v.id, v.input),
    onSuccess: done,
  })
}

export function useCancelWarning() {
  const done = useWarningMutations()
  return useMutation({
    mutationFn: (v: { id: string; reason: string }) => api.warnings.cancel(v.id, v.reason),
    onSuccess: done,
  })
}

export function useSensors() {
  return useQuery({ queryKey: warningKeys.sensors, queryFn: () => api.warnings.simulation.sensors() })
}

export function useSensorTick() {
  const done = useWarningMutations()
  return useMutation({ mutationFn: (id: string) => api.warnings.simulation.tick(id), onSuccess: done })
}

export function useSetSensorReading() {
  const done = useWarningMutations()
  return useMutation({
    mutationFn: (v: { id: string; value: number }) => api.warnings.simulation.setReading(v.id, v.value),
    onSuccess: done,
  })
}

export function useChannelSettings() {
  return useQuery({ queryKey: warningKeys.channels, queryFn: () => api.warnings.simulation.channels() })
}

export function useSetChannel() {
  const done = useWarningMutations()
  return useMutation({
    mutationFn: (v: { channel: Channel; patch: Partial<Pick<ChannelSetting, 'enabled' | 'simulateFailure'>> }) =>
      api.warnings.simulation.setChannel(v.channel, v.patch),
    onSuccess: done,
  })
}
