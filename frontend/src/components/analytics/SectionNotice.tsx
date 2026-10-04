import { DatabaseZap } from 'lucide-react'

/** A section with no data says so and why, instead of showing zeros. */
export function SectionNotice({ name, reason }: { name: string; reason: string }) {
  return (
    <div className="flex items-start gap-3 rounded-card border border-dashed border-line px-4 py-5 text-sm">
      <DatabaseZap className="mt-0.5 size-5 shrink-0 text-faint" aria-hidden />
      <div>
        <p className="font-medium text-ink">{name}: data unavailable</p>
        <p className="text-muted">{reason}</p>
      </div>
    </div>
  )
}
