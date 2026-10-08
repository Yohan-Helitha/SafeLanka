import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { POLL } from '@/constants/app'
import { useOutbox } from '@/context/OutboxContext'
import { api, isApiError } from '@/services'
import type { RejectionReason, ReportFilter, ReportInput, ReportListItem } from '@/types'

export const reportKeys = {
  all: ['reports'] as const,
  mine: ['reports', 'mine'] as const,
  list: (filter: ReportFilter) => ['reports', 'list', filter] as const,
  detail: (id: string) => ['reports', 'detail', id] as const,
}

export function useMyReports() {
  return useQuery({ queryKey: reportKeys.mine, queryFn: () => api.reports.mine() })
}

export function useReportQueue(filter: ReportFilter) {
  return useQuery({
    queryKey: reportKeys.list(filter),
    queryFn: () => api.reports.search(filter),
    refetchInterval: POLL.queue,
  })
}

export function useReport(id: string) {
  return useQuery({ queryKey: reportKeys.detail(id), queryFn: () => api.reports.get(id) })
}

export type SubmitResult = { queued: false; report: ReportListItem } | { queued: true; clientRef: string }

/** Sends a report; when offline it is saved to the outbox and sent automatically later. */
export function useSubmitReport() {
  const queryClient = useQueryClient()
  const { enqueue } = useOutbox()
  return useMutation<SubmitResult, Error, ReportInput>({
    mutationFn: async (input) => {
      try {
        return { queued: false, report: await api.reports.submit(input) }
      } catch (e) {
        if (isApiError(e) && e.isOffline) {
          await enqueue(input.clientRef, { kind: 'REPORT', input }, 'Hazard report')
          return { queued: true, clientRef: input.clientRef }
        }
        throw e
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reportKeys.all }),
  })
}

function useReviewInvalidation() {
  const queryClient = useQueryClient()
  return async () => {
    await queryClient.invalidateQueries({ queryKey: reportKeys.all })
    await queryClient.invalidateQueries({ queryKey: ['warnings', 'hazards'] })
  }
}

export function useVerifyReport() {
  const invalidate = useReviewInvalidation()
  return useMutation({
    mutationFn: (v: { id: string; severity?: number }) => api.reports.verify(v.id, v.severity),
    onSuccess: invalidate,
  })
}

export function useRejectReport() {
  const invalidate = useReviewInvalidation()
  return useMutation({
    mutationFn: (v: { id: string; reason: RejectionReason; comment: string }) =>
      api.reports.reject(v.id, v.reason, v.comment),
    onSuccess: invalidate,
  })
}

export function useRequestInfo() {
  const invalidate = useReviewInvalidation()
  return useMutation({
    mutationFn: (v: { id: string; comment: string }) => api.reports.requestInfo(v.id, v.comment),
    onSuccess: invalidate,
  })
}
