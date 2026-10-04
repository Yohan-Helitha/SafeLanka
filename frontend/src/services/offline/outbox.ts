import { openDB } from 'idb'
import type { DBSchema } from 'idb'
import type { DistributionInput, ReportInput, TeamStatusUpdate } from '@/types'

export type OutboxKind = 'REPORT' | 'TEAM_STATUS' | 'DISTRIBUTION'

export type OutboxPayload =
  | { kind: 'REPORT'; input: ReportInput }
  | { kind: 'TEAM_STATUS'; teamId: string; update: TeamStatusUpdate }
  | { kind: 'DISTRIBUTION'; allocationId: string; input: DistributionInput }

export interface OutboxItem {
  clientRef: string
  kind: OutboxKind
  payload: OutboxPayload
  label: string
  createdAt: string
  attempts: number
  state: 'PENDING' | 'NEEDS_ATTENTION'
  lastError: string | null
}

interface OutboxDb extends DBSchema {
  outbox: { key: string; value: OutboxItem }
}

const dbPromise = openDB<OutboxDb>('safelanka', 1, {
  upgrade(db) {
    db.createObjectStore('outbox', { keyPath: 'clientRef' })
  },
})

export const outboxStore = {
  async all(): Promise<OutboxItem[]> {
    const items = await (await dbPromise).getAll('outbox')
    return items.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  },
  async put(item: OutboxItem): Promise<void> {
    await (await dbPromise).put('outbox', item)
  },
  async remove(clientRef: string): Promise<void> {
    await (await dbPromise).delete('outbox', clientRef)
  },
}
