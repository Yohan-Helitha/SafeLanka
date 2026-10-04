import clsx from 'clsx'
import { NavLink, Outlet } from 'react-router-dom'
import { APP_NAME } from '@/constants/app'
import { CITIZEN_NAV } from '@/navigation/navConfig'
import { OfflineBanner } from './OfflineBanner'
import { OfflineToggle } from './OfflineToggle'
import { RoleSwitcher } from './RoleSwitcher'

interface Props {
  title: string
  /** Citizens and volunteers get a fixed bottom navigation bar. */
  bottomNav?: boolean
}

/** Light, large-target layout for people in the field. */
export function MobileLayout({ title, bottomNav = false }: Props) {
  return (
    <div data-surface="field" className="min-h-screen bg-canvas text-ink">
      <header className="sticky top-0 z-30 bg-topbar text-slate-100 safe-top">
        <div className="mx-auto flex max-w-md items-center justify-between gap-2 px-4 py-2.5">
          <div className="min-w-0">
            <p className="font-display text-lg font-semibold leading-tight">{APP_NAME}</p>
            <p className="truncate text-xs text-cyan-300">{title}</p>
          </div>
          <div className="flex items-center gap-2">
            <OfflineToggle className="border-slate-600 text-slate-300" />
            <RoleSwitcher compact />
          </div>
        </div>
        <OfflineBanner />
      </header>

      <main className={clsx('mx-auto max-w-md px-4 pt-4', bottomNav ? 'pb-28' : 'pb-10')}>
        <Outlet />
      </main>

      {bottomNav && (
        <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-panel safe-bottom">
          <ul className="mx-auto grid max-w-md grid-cols-3">
            {CITIZEN_NAV.map(({ label, to, icon: Icon, end }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    clsx(
                      'flex min-h-[60px] flex-col items-center justify-center gap-0.5 text-xs',
                      isActive ? 'font-semibold text-signal' : 'text-muted',
                    )
                  }
                >
                  <Icon className="size-5" aria-hidden />
                  {label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  )
}
