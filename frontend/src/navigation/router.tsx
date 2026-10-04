import { createBrowserRouter, Navigate } from 'react-router-dom'
import type { RouteObject } from 'react-router-dom'
import { MobileLayout, PortalLayout } from '@/components/layout'
import { paths } from '@/constants/routes'
import { NotFoundScreen } from '@/screens/landing/NotFoundScreen'
import { RoleSelectScreen } from '@/screens/landing/RoleSelectScreen'
import { RequireRole } from './RequireRole'

/** Screens load on demand so the charts and field flows do not weigh down the first paint. */
const screen = (load: () => Promise<Record<string, unknown>>, name: string): Pick<RouteObject, 'lazy'> => ({
  lazy: async () => ({ Component: (await load())[name] as React.ComponentType }),
})

const reportsScreens = {
  hazard: () => import('@/screens/reports/ReportHazardScreen'),
  done: () => import('@/screens/reports/ReportSubmittedScreen'),
  mine: () => import('@/screens/reports/MyReportsScreen'),
  queue: () => import('@/screens/reports/VerificationQueueScreen'),
  detail: () => import('@/screens/reports/ReportDetailScreen'),
}
const warningsScreens = {
  alerts: () => import('@/screens/warnings/CitizenAlertsScreen'),
  alert: () => import('@/screens/warnings/AlertDetailScreen'),
  hazards: () => import('@/screens/warnings/HazardDashboardScreen'),
  hazard: () => import('@/screens/warnings/HazardDetailScreen'),
  list: () => import('@/screens/warnings/WarningsScreen'),
  create: () => import('@/screens/warnings/CreateWarningScreen'),
  result: () => import('@/screens/warnings/WarningResultScreen'),
  simulation: () => import('@/screens/warnings/SimulationScreen'),
}
const responseScreens = {
  team: () => import('@/screens/response/TeamAssignmentScreen'),
  coordinator: () => import('@/screens/response/CoordinatorScreen'),
  dashboard: () => import('@/screens/response/DistrictDashboardScreen'),
  teams: () => import('@/screens/response/RescueTeamsScreen'),
  newAssignment: () => import('@/screens/response/NewAssignmentScreen'),
  shelters: () => import('@/screens/response/DistrictSheltersScreen'),
  relief: () => import('@/screens/response/ReliefSuppliesScreen'),
}

/** Analytics screens are shared; each role passes its own paths. */
const analyticsHome = (reportPath: (id: string) => string, canGenerate: boolean): Pick<RouteObject, 'lazy'> => ({
  lazy: async () => {
    const { AnalyticsHomeScreen } = await import('@/screens/analytics/AnalyticsHomeScreen')
    return { Component: () => <AnalyticsHomeScreen reportPath={reportPath} canGenerate={canGenerate} /> }
  },
})
const analyticsReport = (backTo: string): Pick<RouteObject, 'lazy'> => ({
  lazy: async () => {
    const { AnalysisReportScreen } = await import('@/screens/analytics/AnalysisReportScreen')
    return { Component: () => <AnalysisReportScreen backTo={backTo} /> }
  },
})

export const router = createBrowserRouter([
  { path: paths.landing, element: <RoleSelectScreen /> },

  // Citizens and volunteers: light mobile app with a bottom bar
  {
    element: <RequireRole roles={['CITIZEN', 'VOLUNTEER']} />,
    children: [
      {
        element: <MobileLayout title="Alerts and reports" bottomNav />,
        children: [
          { path: paths.citizen.home, ...screen(warningsScreens.alerts, 'CitizenAlertsScreen') },
          { path: '/app/alerts/:id', ...screen(warningsScreens.alert, 'AlertDetailScreen') },
          { path: paths.citizen.report, ...screen(reportsScreens.hazard, 'ReportHazardScreen') },
          { path: '/app/report/done/:id', ...screen(reportsScreens.done, 'ReportSubmittedScreen') },
          { path: paths.citizen.reports, ...screen(reportsScreens.mine, 'MyReportsScreen') },
        ],
      },
    ],
  },

  // Rescue team member
  {
    element: <RequireRole roles={['RESCUE_MEMBER']} />,
    children: [
      {
        element: <MobileLayout title="Rescue team" />,
        children: [{ path: paths.team, ...screen(responseScreens.team, 'TeamAssignmentScreen') }],
      },
    ],
  },

  // Shelter coordinator
  {
    element: <RequireRole roles={['SHELTER_COORDINATOR']} />,
    children: [
      {
        element: <MobileLayout title="Shelter" />,
        children: [{ path: paths.coordinator, ...screen(responseScreens.coordinator, 'CoordinatorScreen') }],
      },
    ],
  },

  // DMC duty officer: dark portal
  {
    element: <RequireRole roles={['DMC_OFFICER']} />,
    children: [
      {
        element: <PortalLayout />,
        children: [
          { path: paths.dmc.home, element: <Navigate to={paths.dmc.hazards} replace /> },
          { path: paths.dmc.hazards, ...screen(warningsScreens.hazards, 'HazardDashboardScreen') },
          { path: '/dmc/hazards/:id', ...screen(warningsScreens.hazard, 'HazardDetailScreen') },
          { path: paths.dmc.reports, ...screen(reportsScreens.queue, 'VerificationQueueScreen') },
          { path: '/dmc/reports/:id', ...screen(reportsScreens.detail, 'ReportDetailScreen') },
          { path: paths.dmc.warnings, ...screen(warningsScreens.list, 'WarningsScreen') },
          { path: paths.dmc.newWarning, ...screen(warningsScreens.create, 'CreateWarningScreen') },
          { path: '/dmc/warnings/:id', ...screen(warningsScreens.result, 'WarningResultScreen') },
          { path: paths.dmc.analytics, ...analyticsHome(paths.dmc.analysis, true) },
          { path: '/dmc/analytics/:id', ...analyticsReport(paths.dmc.analytics) },
          { path: paths.dmc.simulation, ...screen(warningsScreens.simulation, 'SimulationScreen') },
        ],
      },
    ],
  },

  // District officer: dark portal, read-only analysis
  {
    element: <RequireRole roles={['DISTRICT_OFFICER']} />,
    children: [
      {
        element: <PortalLayout />,
        children: [
          { path: paths.district.home, ...screen(responseScreens.dashboard, 'DistrictDashboardScreen') },
          { path: paths.district.teams, ...screen(responseScreens.teams, 'RescueTeamsScreen') },
          { path: paths.district.newAssignment, ...screen(responseScreens.newAssignment, 'NewAssignmentScreen') },
          { path: paths.district.shelters, ...screen(responseScreens.shelters, 'DistrictSheltersScreen') },
          { path: paths.district.relief, ...screen(responseScreens.relief, 'ReliefSuppliesScreen') },
          { path: paths.district.analytics, ...analyticsHome(paths.district.analysis, false) },
          { path: '/district/analytics/:id', ...analyticsReport(paths.district.analytics) },
        ],
      },
    ],
  },

  { path: '*', element: <NotFoundScreen /> },
])
