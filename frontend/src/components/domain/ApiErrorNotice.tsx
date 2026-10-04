import { CircleAlert, WifiOff } from 'lucide-react'
import type { ReactNode } from 'react'
import { isApiError } from '@/services/ApiError'

interface Props {
  error: unknown
  /** Extra content, for example a link to resolve a CONFLICT. */
  children?: ReactNode
}

/** Says what happened and what to do, next to the action that failed. */
export function ApiErrorNotice({ error, children }: Props) {
  if (!error) return null
  const api = isApiError(error) ? error : null
  const fields = api ? Object.values(api.fieldErrors) : []
  const Icon = api?.isOffline ? WifiOff : CircleAlert
  return (
    <div role="alert" className="flex gap-3 rounded-control border border-danger/50 bg-danger/10 px-3 py-2.5 text-sm">
      <Icon className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden />
      <div className="min-w-0 text-ink">
        <p>{api ? api.message : 'Something went wrong. Try again.'}</p>
        {fields.length > 0 && (
          <ul className="mt-1 list-disc pl-4 text-muted">
            {fields.map((f) => (
              <li key={f}>{f}</li>
            ))}
          </ul>
        )}
        {children}
      </div>
    </div>
  )
}
