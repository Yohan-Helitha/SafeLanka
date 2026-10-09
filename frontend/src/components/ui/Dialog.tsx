import clsx from 'clsx'
import { X } from 'lucide-react'
import { useEffect, useId, useRef } from 'react'
import type { ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  footer?: ReactNode
  children?: ReactNode
  className?: string
}

/** Modal built on the native <dialog>: focus trap and Escape come from the browser. */
export function Dialog({ open, onClose, title, description, footer, children, className }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (open && !el.open) el.showModal()
    if (!open && el.open) el.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()}
      className={clsx('m-auto w-[calc(100%-1.25rem)] sm:w-[calc(100%-2rem)] rounded-card border border-line p-0 shadow-2xl', className)}
    >
      {open && (
        <div className="flex max-h-[85vh] flex-col">
          <header className="flex items-start justify-between gap-3 border-b border-line px-4 py-3 sm:px-5 sm:py-4">
            <div>
              <h2 id={titleId} className="font-display text-lg font-semibold text-ink sm:text-xl">
                {title}
              </h2>
              {description && <p className="mt-1 text-xs text-muted sm:text-sm">{description}</p>}
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close"
              className="-mr-1 grid size-9 shrink-0 place-items-center rounded-control text-muted hover:bg-raised hover:text-ink"
            >
              <X className="size-5" aria-hidden />
            </button>
          </header>
          <div className="overflow-y-auto custom-scrollbar px-4 py-3 sm:px-5 sm:py-4">{children}</div>
          {footer && <footer className="flex flex-col-reverse gap-2 border-t border-line px-4 py-3 sm:flex-row sm:flex-wrap sm:justify-end sm:px-5 sm:py-4">{footer}</footer>}
        </div>
      )}
    </dialog>
  )
}
