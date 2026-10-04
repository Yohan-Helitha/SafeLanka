import { CircleAlert, Loader2 } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button } from './Button'

export function Loading({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted" role="status">
      <Loader2 className="size-5 animate-spin" aria-hidden />
      <span>{label}</span>
    </div>
  )
}

interface EmptyProps {
  icon?: LucideIcon
  title: string
  description?: ReactNode
  action?: ReactNode
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyProps) {
  return (
    <div className="flex flex-col items-center rounded-card border border-dashed border-line px-6 py-10 text-center">
      {Icon && <Icon className="mb-3 size-9 text-faint" aria-hidden />}
      <h3 className="font-display text-lg font-semibold text-ink">{title}</h3>
      {description && <p className="mt-1 max-w-sm text-sm text-muted">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

interface ErrorProps {
  error?: unknown
  title?: string
  onRetry?: () => void
}

export function ErrorState({ error, title = 'We could not load this', onRetry }: ErrorProps) {
  const message = error instanceof Error ? error.message : 'Check your connection and try again.'
  return (
    <div className="flex flex-col items-center rounded-card border border-danger/40 bg-danger/5 px-6 py-10 text-center" role="alert">
      <CircleAlert className="mb-3 size-9 text-danger" aria-hidden />
      <h3 className="font-display text-lg font-semibold text-ink">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-muted">{message}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
