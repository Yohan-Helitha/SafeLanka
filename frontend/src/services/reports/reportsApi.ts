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
  verify(id: string): Promise<ReportDetail>
  reject(id: string, reason: RejectionReason, comment: string): Promise<ReportDetail>
  requestInfo(id: string, comment: string): Promise<ReportDetail>
}
