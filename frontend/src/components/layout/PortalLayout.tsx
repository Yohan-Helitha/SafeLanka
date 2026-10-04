import clsx from 'clsx'
import { NavLink, Outlet } from 'react-router-dom'
import { APP_NAME } from '@/constants/app'
import { useCurrentUser } from '@/context/AuthContext'
import { useReferenceData } from '@/hooks/shared'
import { PORTAL_NAV } from '@/navigation/navConfig'
import { OfflineBanner } from './OfflineBanner'
import { OfflineToggle } from './OfflineToggle'
import { UserMenu } from './UserMenu'
import { SituationStrip } from './SituationStrip'

/** Dark control-room layout for DMC and district officers. */
export function PortalLayout() {
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const items = PORTAL_NAV[user.role] ?? []
  const subtitle = user.role === 'DMC_OFFICER' ? 'DMC command' : `${districtName(user.districtId)} district`

  return (
    <div data-surface="portal" className="min-h-screen bg-canvas text-ink md:flex">
      <aside className="hidden w-60 shrink-0 flex-col bg-sidebar md:flex">
        <div className="px-5 py-5">
          <p className="font-display text-2xl font-semibold text-ink">{APP_NAME}</p>
          <p className="text-sm text-signal">{subtitle}</p>
        </div>
        <nav aria-label="Main" className="flex-1 space-y-1 px-3">
          {items.map(({ label, to, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                clsx(
                  'flex min-h-11 items-center gap-3 rounded-control px-3 text-[15px] transition-colors',
                  isActive ? 'bg-signal/15 font-medium text-signal' : 'text-muted hover:bg-raised hover:text-ink',
                )
              }
            >
              <Icon className="size-[18px]" aria-hidden />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-line p-3">
          <UserMenu />
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-30 bg-canvas">
          <div className="flex items-center justify-between gap-2 border-b border-line bg-sidebar px-4 py-2 md:hidden">
            <div>
              <span className="font-display text-lg font-semibold">{APP_NAME}</span>
              <span className="ml-2 text-xs text-signal">{subtitle}</span>
            </div>
            <div className="flex items-center gap-2">
              <OfflineToggle />
              <UserMenu compact />
            </div>
          </div>
          <nav aria-label="Main" className="flex gap-1 overflow-x-auto border-b border-line bg-sidebar px-2 md:hidden">
            {items.map(({ label, to, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  clsx(
                    'whitespace-nowrap border-b-2 px-3 py-2.5 text-sm',
                    isActive ? 'border-signal font-medium text-signal' : 'border-transparent text-muted',
                  )
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>
          <SituationStrip />
        </header>
        <OfflineBanner />
        <main className="mx-auto max-w-6xl px-4 py-6 md:px-8">
          <div className="mb-4 hidden justify-end md:flex">
            <OfflineToggle />
          </div>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
