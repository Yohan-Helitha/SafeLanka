import type { AnalyticsEvent, DisasterReport, ReportFilters, ReportSummary } from '@/types'

export type ExportFormat = 'PDF' | 'CSV'

export interface AnalyticsApi {
  events(): Promise<AnalyticsEvent[]>
  generate(eventId: string, filters: ReportFilters): Promise<DisasterReport>
  list(): Promise<ReportSummary[]>
  get(id: string): Promise<DisasterReport>
  download(id: string, format: ExportFormat): Promise<Blob>
}
