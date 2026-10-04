import {
  BarChart3,
  Bell,
  ClipboardList,
  FlaskConical,
  LayoutDashboard,
  Megaphone,
  Package,
  Plus,
  Radar,
  School,
  Users,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { paths } from '@/constants/routes'
import type { Role } from '@/types'

export interface NavItem {
  label: string
  to: string
  icon: LucideIcon
  /** Match the path exactly (used for index-like routes). */
  end?: boolean
}

/** Sidebar items for each portal (dark) role. */
export const PORTAL_NAV: Partial<Record<Role, NavItem[]>> = {
  DMC_OFFICER: [
    { label: 'Hazards', to: paths.dmc.hazards, icon: Radar },
    { label: 'Ground reports', to: paths.dmc.reports, icon: ClipboardList },
    { label: 'Warnings', to: paths.dmc.warnings, icon: Megaphone },
    { label: 'Analysis', to: paths.dmc.analytics, icon: BarChart3 },
    { label: 'Simulation', to: paths.dmc.simulation, icon: FlaskConical },
  ],
  DISTRICT_OFFICER: [
    { label: 'Dashboard', to: paths.district.home, icon: LayoutDashboard, end: true },
    { label: 'Rescue teams', to: paths.district.teams, icon: Users },
    { label: 'Shelters', to: paths.district.shelters, icon: School },
    { label: 'Relief supplies', to: paths.district.relief, icon: Package },
    { label: 'Analysis', to: paths.district.analytics, icon: BarChart3 },
  ],
}

/** Fixed bottom bar for citizens and volunteers. */
export const CITIZEN_NAV: NavItem[] = [
  { label: 'Alerts', to: paths.citizen.home, icon: Bell, end: true },
  { label: 'Report', to: paths.citizen.report, icon: Plus },
  { label: 'My reports', to: paths.citizen.reports, icon: ClipboardList },
]
