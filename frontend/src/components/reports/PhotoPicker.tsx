import { Camera, X } from 'lucide-react'
import { useEffect, useId, useMemo } from 'react'
import { Button } from '../ui'

interface Props {
  file: File | null
  onChange: (file: File | null) => void
  error?: string | null
}

export function PhotoPicker({ file, onChange, error }: Props) {
  const inputId = useId()
  const url = useMemo(() => (file ? URL.createObjectURL(file) : null), [file])
  useEffect(() => () => (url ? URL.revokeObjectURL(url) : undefined), [url])

  return (
    <div>
      {url ? (
        <div className="relative overflow-hidden rounded-control border border-line">
          <img src={url} alt="Photo you added to the report" className="max-h-56 w-full object-cover" />
          <Button
            variant="secondary"
            size="sm"
            className="absolute right-2 top-2"
            icon={<X className="size-4" aria-hidden />}
            onClick={() => onChange(null)}
          >
            Remove
          </Button>
        </div>
      ) : (
        <label
          htmlFor={inputId}
          className="flex min-h-[96px] cursor-pointer flex-col items-center justify-center gap-1 rounded-control border-2 border-dashed border-line bg-raised px-4 py-5 text-center text-muted hover:border-signal/60"
        >
          <Camera className="size-6" aria-hidden />
          <span className="font-medium text-ink">Take or add a photo</span>
          <span className="text-xs">JPEG or PNG, up to 5 MB</span>
        </label>
      )}
      <input
        id={inputId}
        type="file"
        accept="image/jpeg,image/png"
        capture="environment"
        className="sr-only"
        onChange={(e) => {
          onChange(e.target.files?.[0] ?? null)
          e.target.value = ''
        }}
      />
      {error && <p className="mt-1 text-sm text-danger">{error}</p>}
    </div>
  )
}
