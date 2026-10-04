import clsx from 'clsx'
import { useId } from 'react'
import type { ReactNode } from 'react'

export interface SegmentedOption<T extends string | number> {
  value: T
  label: ReactNode
  description?: ReactNode
  disabled?: boolean
}

interface SegmentedProps<T extends string | number> {
  legend: string
  value: T | null
  onChange: (value: T) => void
  options: SegmentedOption<T>[]
  columns?: 1 | 2 | 3 | 4 | 5
  error?: string | null
  hideLegend?: boolean
  className?: string
}

const COLS = {
  1: 'grid-cols-1',
  2: 'grid-cols-2',
  3: 'grid-cols-3',
  4: 'grid-cols-2 sm:grid-cols-4',
  5: 'grid-cols-5',
}

/** Radio group drawn as large selectable segments. */
export function Segmented<T extends string | number>({
  legend,
  value,
  onChange,
  options,
  columns = 2,
  error,
  hideLegend,
  className,
}: SegmentedProps<T>) {
  const name = useId()
  return (
    <fieldset className={className} aria-invalid={error ? true : undefined}>
      <legend className={clsx('mb-1.5 text-sm font-medium text-ink', hideLegend && 'sr-only')}>{legend}</legend>
      <div className={clsx('grid gap-2', COLS[columns])}>
        {options.map((o) => (
          <label key={String(o.value)} className={clsx('block', o.disabled && 'opacity-50')}>
            <input
              type="radio"
              name={name}
              className="peer sr-only"
              checked={value === o.value}
              disabled={o.disabled}
              onChange={() => onChange(o.value)}
            />
            <span
              className={clsx(
                'flex min-h-[44px] cursor-pointer flex-col justify-center rounded-control border border-line bg-raised px-3 py-2 text-[15px] text-ink',
                'transition-colors hover:border-signal/60 peer-checked:border-signal peer-checked:bg-signal/10',
                'peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-[rgb(var(--signal))]',
              )}
            >
              <span className="font-medium">{o.label}</span>
              {o.description && <span className="text-sm text-muted">{o.description}</span>}
            </span>
          </label>
        ))}
      </div>
      {error && <p className="mt-1 text-sm text-danger">{error}</p>}
    </fieldset>
  )
}
