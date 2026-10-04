import clsx from 'clsx'

/** Five small pips plus "3/5". Cyan up to 2, amber at 3, red from 4. */
export function SeverityMeter({ value }: { value: number }) {
  const tone = value >= 4 ? 'bg-danger' : value === 3 ? 'bg-caution' : 'bg-signal'
  return (
    <span className="inline-flex items-center gap-2" role="img" aria-label={`Severity ${value} of 5`}>
      <span className="flex gap-0.5" aria-hidden>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={clsx('h-3 w-1.5 rounded-sm', n <= value ? tone : 'bg-line')} />
        ))}
      </span>
      <span className="tabular text-sm text-muted">{value}/5</span>
    </span>
  )
}
