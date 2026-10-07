import clsx from 'clsx'
import type { ReactNode } from 'react'

interface CardProps {
  title?: ReactNode
  description?: ReactNode
  actions?: ReactNode
  padded?: boolean
  className?: string
  contentClassName?: string
  children?: ReactNode
}

export function Card({ title, description, actions, padded = true, className, contentClassName, children }: CardProps) {
  return (
    <section className={clsx('rounded-card border border-line bg-panel', className)}>
      {(title || actions) && (
        <header className="flex items-start justify-between gap-3 px-4 pt-4">
          <div className="min-w-0">
            {title && <h2 className="text-base font-semibold text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
          </div>
          {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
        </header>
      )}
      {children !== undefined && (
        <div className={clsx(padded && 'p-4', (title || actions) && padded && 'pt-3', contentClassName)}>
          {children}
        </div>
      )}
    </section>
  )
}
