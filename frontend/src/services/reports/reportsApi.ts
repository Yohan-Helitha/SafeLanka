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
  /** The reporter answers the officer's question, optionally with a photo that replaces the old one. */
  reply(id: string, message: string, photo?: File | null): Promise<ReportDetail>
}
