import type {
  Channel,
  HazardSource,
  OrganisationType,
  RejectionReason,
  ReportStatus,
  TeamType,
  WarningLevel,
} from '@/types'

export const CATEGORY_LABEL: Record<string, string> = {
  RISING_WATER: 'Rising water',
  BLOCKED_ROAD: 'Blocked road',
  LANDSLIDE_CRACK: 'Landslide crack',
  WATER_SHORTAGE: 'Water shortage',
  OTHER: 'Something else',
}

export const REJECTION_LABEL: Record<RejectionReason, string> = {
  INSUFFICIENT_EVIDENCE: 'Not enough evidence',
  DUPLICATE: 'Duplicate of another report',
  LOCATION_MISMATCH: 'Location does not match',
  NOT_A_HAZARD: 'Not a hazard',
  OTHER: 'Other (explain)',
}

/** Wording shown to citizens for each report status. */
export const CITIZEN_REPORT_STATUS: Record<ReportStatus, string> = {
  PENDING: 'Waiting for review',
  NEEDS_MORE_INFO: 'More information needed',
  VERIFIED: 'Verified',
  REJECTED: 'Not accepted',
}

export const HAZARD_SOURCE_LABEL: Record<HazardSource, string> = {
  MANUAL: 'Officer entry',
  SENSOR: 'River gauge',
  REPORT: 'Ground reports',
}

export const CHANNEL_LABEL: Record<Channel, string> = {
  PUSH: 'App notification',
  SMS: 'SMS',
  AUDIBLE: 'Audible alarm',
}

export const ORGANISATION_TYPE_LABEL: Record<OrganisationType, string> = {
  GOVERNMENT: 'Government',
  ARMED_FORCES: 'Armed forces',
  POLICE: 'Police',
  NGO: 'NGO',
  PRIVATE_DONOR: 'Private donor',
}

export const TEAM_TYPE_LABEL: Record<TeamType, string> = {
  BOAT: 'Boat team',
  MEDICAL: 'Medical team',
  SEARCH: 'Search team',
}

export const PRIORITY_LABEL: Record<1 | 2 | 3, string> = {
  1: 'Urgent',
  2: 'High',
  3: 'Routine',
}

export const LEVEL_ORDER: WarningLevel[] = ['ADVISORY', 'WATCH', 'WARNING', 'EVACUATE']
