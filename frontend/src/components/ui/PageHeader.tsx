import { ChevronLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface PageHeaderProps {
  title: ReactNode
  subtitle?: ReactNode
  backTo?: string
  backLabel?: string
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, backTo, backLabel = 'Back', actions }: PageHeaderProps) {
  return (
    <header className="mb-5">
      {backTo && (
        <Link to={backTo} className="mb-2 inline-flex items-center gap-1 text-sm text-muted hover:text-signal">
          <ChevronLeft className="size-4" aria-hidden />
          {backLabel}
        </Link>
      )}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h1 className="font-display text-[28px] font-semibold leading-tight text-ink">{title}</h1>
          {subtitle && <p className="mt-1 max-w-3xl text-muted">{subtitle}</p>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  )
}
