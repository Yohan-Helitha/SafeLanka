/** Tiny in-browser event bus mirroring the Spring Modulith events between modules. */
export type MockEvent =
  | { type: 'ReportVerified'; reportId: string }
  | { type: 'WarningPublished'; warningId: string }
  | { type: 'WarningEscalated'; warningId: string }
  | { type: 'WarningCancelled'; warningId: string }

type Handler = (event: MockEvent) => void

const handlers: Handler[] = []

export const bus = {
  subscribe(handler: Handler): void {
    handlers.push(handler)
  },
  emit(event: MockEvent): void {
    handlers.forEach((h) => h(event))
  },
}
