import { env } from './env'

export const paths = {
  landing: '/',
  auth: {
    login: '/login',
    signup: '/signup',
    verify: '/verify',
  },
  citizen: {
    home: '/app',
    alert: (id: string) => `/app/alerts/${id}`,
    report: '/app/report',
    reportDone: (id: string) => `/app/report/done/${id}`,
    reports: '/app/reports',
  },
  team: '/team',
  coordinator: '/coordinator',
  dmc: {
    home: '/dmc',
    hazards: '/dmc/hazards',
    hazard: (id: string) => `/dmc/hazards/${id}`,
    reports: '/dmc/reports',
    report: (id: string) => `/dmc/reports/${id}`,
    warnings: '/dmc/warnings',
    newWarning: '/dmc/warnings/new',
    warning: (id: string) => `/dmc/warnings/${id}`,
    analytics: '/dmc/analytics',
    analysis: (id: string) => `/dmc/analytics/${id}`,
    simulation: '/dmc/simulation',
  },
  district: {
    home: '/district',
    teams: '/district/teams',
    newAssignment: '/district/assignments/new',
    shelters: '/district/shelters',
    relief: '/district/relief',
    analytics: '/district/analytics',
    analysis: (id: string) => `/district/analytics/${id}`,
  },
} as const

/** Where someone who is not signed in is sent: the login screen, or the role picker in demo mode. */
export const entryPath: string = env.authMode === 'login' ? paths.auth.login : paths.landing
