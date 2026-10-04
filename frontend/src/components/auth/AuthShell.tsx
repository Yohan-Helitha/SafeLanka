import { ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { APP_NAME } from '@/constants/app'

interface Props {
  title: string
  subtitle?: ReactNode
  children: ReactNode
  footer?: ReactNode
}

/** Centered card on the dark surface, shared by the login, sign-up and verify screens. */
export function AuthShell({ title, subtitle, children, footer }: Props) {
  return (
    <div data-surface="portal" className="min-h-screen bg-canvas px-4 py-8 text-ink">
      <main className="mx-auto w-full max-w-md">
        <div className="mb-6 text-center">
          <ShieldCheck className="mx-auto size-10 text-signal" aria-hidden />
          <p className="mt-2 font-display text-3xl font-semibold">{APP_NAME}</p>
          <p className="text-sm text-muted">Smart early warning for Sri Lanka</p>
        </div>
        <div className="rounded-card border border-line bg-panel p-5">
          <h1 className="font-display text-2xl font-semibold">{title}</h1>
          {subtitle && <p className="mb-4 mt-1 text-sm text-muted">{subtitle}</p>}
          {!subtitle && <div className="mb-4" />}
          {children}
        </div>
        {footer && <div className="mt-4 text-center text-sm text-muted">{footer}</div>}
      </main>
    </div>
  )
}
