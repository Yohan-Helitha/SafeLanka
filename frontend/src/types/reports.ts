import type { Id, LatLng } from './common'

export type ReportStatus = 'PENDING' | 'NEEDS_MORE_INFO' | 'VERIFIED' | 'REJECTED'
export type RejectionReason =
  | 'INSUFFICIENT_EVIDENCE'
  | 'DUPLICATE'
  | 'LOCATION_MISMATCH'
  | 'NOT_A_HAZARD'
  | 'OTHER'

export interface ReportInput {
  clientRef: Id
  hazardTypeId: Id
  category: string
  description: string
  latitude: number | null
  longitude: number | null
  isManualLocation: boolean
  manualLocationText: string | null
  districtId: Id
  capturedAt: string
  photo: Blob | null
}

export interface ReportListItem {
  id: Id
  referenceNo: string
  hazardTypeId: Id
  category: string
  description: string
  districtId: Id
  status: ReportStatus
  capturedAt: string
  syncedAt: string
  hasPhoto: boolean
  hasDuplicates: boolean
  rejectionReason: RejectionReason | null
  reviewComment: string | null
}

export interface ReportDuplicate {
  id: Id
  referenceNo: string
  distanceMetres: number
}

export interface ReportDetail extends ReportListItem, Partial<LatLng> {
  isManualLocation: boolean
  manualLocationText: string | null
  photoUrl: string | null
  reporter: { id: Id; fullName: string; role: string }
  reviewedBy: string | null
  reviewedAt: string | null
  possibleDuplicates: ReportDuplicate[]
  /** The reporter's answer to the officer's question, once given. */
  reporterReply?: string | null
  repliedAt?: string | null
}

export interface ReportFilter {
  status?: ReportStatus | 'ALL'
  hazardTypeId?: Id
  districtId?: Id
  page?: number
  size?: number
}
