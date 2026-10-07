import clsx from 'clsx'
import { Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { APP_NAME } from '@/constants/app'
import { useCurrentUser } from '@/context/AuthContext'
import { useReferenceData } from '@/hooks/shared'
import { PORTAL_NAV } from '@/navigation/navConfig'
import { OfflineBanner } from './OfflineBanner'
import { OfflineToggle } from './OfflineToggle'
import { SituationStrip } from './SituationStrip'
import { UserMenu } from './UserMenu'

/** Dark control-room layout for DMC and district officers with overlay sidebar drawer on mobile. */
export function PortalLayout() {
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  const items = PORTAL_NAV[user.role] ?? []
  const subtitle = user.role === 'DMC_OFFICER' ? 'DMC command' : `${districtName(user.districtId)} district`

  // Close drawer on route change
  useEffect(() => {
    setMobileNavOpen(false)
  }, [location.pathname])

  // Prevent background scroll when mobile sidebar is open
  useEffect(() => {
    if (mobileNavOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileNavOpen])

  // Close on Escape key
  useEffect(() => {
    if (!mobileNavOpen) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileNavOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [mobileNavOpen])

  return (
    <div data-surface="portal" className="min-h-screen bg-canvas text-ink md:flex">
      {/* Desktop Persistent Sidebar */}
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col overflow-y-auto bg-sidebar md:flex">
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

      {/* Mobile Drawer Backdrop Overlay */}
      {mobileNavOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm md:hidden animate-in fade-in duration-200"
          onClick={() => setMobileNavOpen(false)}
          aria-hidden
        />
      )}

      {/* Mobile Overlay Sidebar Drawer (Overlaying on top) */}
      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-50 flex w-72 max-w-[85vw] flex-col bg-sidebar border-r border-line shadow-2xl transition-transform duration-300 ease-in-out md:hidden',
          mobileNavOpen ? 'translate-x-0' : '-translate-x-full',
        )}
        aria-label="Mobile Navigation"
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <div>
            <p className="font-display text-xl font-semibold text-ink">{APP_NAME}</p>
            <p className="text-xs text-signal font-medium">{subtitle}</p>
          </div>
          <button
            type="button"
            onClick={() => setMobileNavOpen(false)}
            aria-label="Close navigation"
            className="grid size-9 place-items-center rounded-control text-muted hover:bg-raised hover:text-ink active:bg-raised transition-colors"
          >
            <X className="size-5" aria-hidden />
          </button>
        </div>
        <nav aria-label="Main Mobile" className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {items.map(({ label, to, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={() => setMobileNavOpen(false)}
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
        <header className="sticky top-0 z-30 flex flex-col bg-canvas">
          {/* Mobile Header with Hamburger Icon */}
          <div className="flex items-center justify-between gap-2 border-b border-line bg-sidebar px-3 py-2.5 md:hidden">
            <div className="flex items-center gap-2.5 min-w-0">
              <button
                type="button"
                onClick={() => setMobileNavOpen(true)}
                aria-label="Open navigation menu"
                className="grid size-9 shrink-0 place-items-center rounded-control text-ink hover:bg-raised active:bg-raised transition-colors"
              >
                <Menu className="size-5" aria-hidden />
              </button>
              <div className="min-w-0 truncate">
                <span className="font-display text-base font-semibold leading-none">{APP_NAME}</span>
                <span className="ml-2 text-xs text-signal font-medium truncate">{subtitle}</span>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <OfflineToggle />
              <UserMenu compact />
            </div>
          </div>
          <SituationStrip />
        </header>
        <OfflineBanner />
        <main className="px-3.5 py-4 sm:px-6 sm:py-6 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
