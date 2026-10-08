import type {
  Page,
  RejectionReason,
  ReportDetail,
  ReportFilter,
  ReportInput,
  ReportListItem,
} from '@/types'

export interface ReportsApi {
  submit(input: ReportInput): Promise<ReportListItem>
  mine(): Promise<ReportListItem[]>
  search(filter: ReportFilter): Promise<Page<ReportListItem>>
  get(id: string): Promise<ReportDetail>
  /** `severity` is how dangerous the officer judges the hazard, 1 (low) to 5 (very dangerous). */
  verify(id: string, severity?: number): Promise<ReportDetail>
  reject(id: string, reason: RejectionReason, comment: string): Promise<ReportDetail>
  requestInfo(id: string, comment: string): Promise<ReportDetail>
}
