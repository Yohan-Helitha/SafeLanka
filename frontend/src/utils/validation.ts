export type FieldErrors = Record<string, string>

export function lengthError(label: string, value: string, min: number, max: number): string | null {
  const n = value.trim().length
  if (n < min) return `${label} needs at least ${min} characters.`
  if (n > max) return `${label} can be at most ${max} characters.`
  return null
}

/** Drops null entries so `Object.keys(errors).length === 0` means valid. */
export function compact(errors: Record<string, string | null | undefined>): FieldErrors {
  return Object.fromEntries(Object.entries(errors).filter(([, v]) => Boolean(v))) as FieldErrors
}
