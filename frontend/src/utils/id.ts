/** Idempotency key for offline-capable submissions. */
export function newClientRef(): string {
  return crypto.randomUUID()
}
