import { env } from '@/constants/env'
import { analyticsHttp } from './analytics/analytics.http'
import { analyticsMock } from './analytics/analytics.mock'
import type { AnalyticsApi } from './analytics/analyticsApi'
import { referenceHttp } from './reference/reference.http'
import { referenceMock } from './reference/reference.mock'
import type { ReferenceApi } from './reference/referenceApi'
import { reportsHttp } from './reports/reports.http'
import { reportsMock } from './reports/reports.mock'
import type { ReportsApi } from './reports/reportsApi'
import { responseHttp } from './response/response.http'
import { responseMock } from './response/response.mock'
import type { ResponseApi } from './response/responseApi'
import { warningsHttp } from './warnings/warnings.http'
import { warningsMock } from './warnings/warnings.mock'
import type { WarningsApi } from './warnings/warningsApi'

export interface Api {
  reference: ReferenceApi
  reports: ReportsApi
  warnings: WarningsApi
  response: ResponseApi
  analytics: AnalyticsApi
}

/** The only data access screens use. VITE_USE_MOCKS switches between the in-browser mocks and the Spring Boot API. */
export const api: Api = env.useMocks
  ? { reference: referenceMock, reports: reportsMock, warnings: warningsMock, response: responseMock, analytics: analyticsMock }
  : { reference: referenceHttp, reports: reportsHttp, warnings: warningsHttp, response: responseHttp, analytics: analyticsHttp }

export { ApiError, isApiError } from './ApiError'
export type { ExportFormat } from './analytics/analyticsApi'
export type { SensorTick } from './warnings/warningsApi'
