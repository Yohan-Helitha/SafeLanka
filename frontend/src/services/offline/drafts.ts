import { openDB } from 'idb'
import type { DBSchema } from 'idb'

/**
 * A report the person has started but not sent. Kept on this device only and never sent by itself
 * (unlike the outbox, which sends whatever it holds). The id doubles as the clientRef when the
 * report is finally sent, so sending twice can never create two reports.
 */
export interface ReportDraft {
  id: string
  ownerId: string
  hazardTypeId: string | null
  category: string | null
  description: string
  photo: Blob | null
  /** The person chose to describe the place instead of sharing GPS. */
  manual: boolean
  manualText: string
  districtId: string
  latitude: number | null
  longitude: number | null
  /** When the draft was started: used as the time the hazard was seen. */
  createdAt: string
  updatedAt: string
}

interface DraftDb extends DBSchema {
  drafts: { key: string; value: ReportDraft }
}

const dbPromise = openDB<DraftDb>('safelanka-drafts', 1, {
  upgrade(db) {
    db.createObjectStore('drafts', { keyPath: 'id' })
  },
})

const listeners = new Set<() => void>()
const notify = () => listeners.forEach((listener) => listener())

export const draftStore = {
  async forOwner(ownerId: string): Promise<ReportDraft[]> {
    const all = await (await dbPromise).getAll('drafts')
    return all.filter((d) => d.ownerId === ownerId).sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  },
  async get(id: string): Promise<ReportDraft | undefined> {
    return (await dbPromise).get('drafts', id)
  },
  async put(draft: ReportDraft): Promise<void> {
    await (await dbPromise).put('drafts', draft)
    notify()
  },
  async remove(id: string): Promise<void> {
    await (await dbPromise).delete('drafts', id)
    notify()
  },
  /** Called whenever a draft is saved or removed, so lists stay current. */
  subscribe(listener: () => void): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
