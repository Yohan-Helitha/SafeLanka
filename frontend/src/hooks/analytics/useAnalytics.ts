import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/services'
import type { ExportFormat } from '@/services'
import type { ReportFilters } from '@/types'

export const analyticsKeys = {
  all: ['analytics'] as const,
  events: ['analytics', 'events'] as const,
  reports: ['analytics', 'reports'] as const,
  report: (id: string) => ['analytics', 'report', id] as const,
}

export function useAnalyticsEvents() {
  return useQuery({ queryKey: analyticsKeys.events, queryFn: () => api.analytics.events() })
}

export function useSavedReports() {
  return useQuery({ queryKey: analyticsKeys.reports, queryFn: () => api.analytics.list() })
}

export function useDisasterReport(id: string) {
  return useQuery({ queryKey: analyticsKeys.report(id), queryFn: () => api.analytics.get(id) })
}

export function useGenerateReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (v: { eventId: string; filters: ReportFilters }) => api.analytics.generate(v.eventId, v.filters),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: analyticsKeys.reports }),
  })
}

export function useDownloadReport() {
  return useMutation({
    mutationFn: async (v: { id: string; format: ExportFormat; filename: string }) => {
      const blob = await api.analytics.download(v.id, v.format)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${v.filename}.${v.format.toLowerCase()}`
      document.body.appendChild(a)
      a.click()
      a.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    },
  })
}
