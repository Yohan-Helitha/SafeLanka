import { useEffect, useState } from 'react'
import { LIMITS } from '@/constants/app'
import { useAuth } from '@/context/AuthContext'
import { draftStore } from '@/services/offline/drafts'
import type { ReportDraft } from '@/services/offline/drafts'
import type { ReportInput } from '@/types'

/** The signed-in person's saved (not yet sent) reports, newest first. */
export function useReportDrafts() {
  const { user } = useAuth()
  const ownerId = user?.id
  const [drafts, setDrafts] = useState<ReportDraft[]>([])

  useEffect(() => {
    let active = true
    const load = async () => {
      let list: ReportDraft[] = []
      try {
        list = ownerId ? await draftStore.forOwner(ownerId) : []
      } catch {
        /* storage blocked (for example a private window): drafts are a convenience only */
      }
      if (active) setDrafts(list)
    }
    void load()
    const unsubscribe = draftStore.subscribe(() => void load())
    return () => {
      active = false
      unsubscribe()
    }
  }, [ownerId])

  return { drafts }
}

/** Saves a draft; failing to save must never stop the person from sending the report. */
export async function saveDraft(draft: ReportDraft): Promise<void> {
  try {
    await draftStore.put(draft)
  } catch {
    /* storage unavailable */
  }
}

export async function removeDraft(id: string): Promise<void> {
  try {
    await draftStore.remove(id)
  } catch {
    /* storage unavailable */
  }
}

/** The report to send, or null while the draft is still missing something the server requires. */
export function draftToInput(draft: ReportDraft): ReportInput | null {
  const hasGps = !draft.manual && draft.latitude !== null && draft.longitude !== null
  const manualOk = draft.manual && draft.manualText.trim().length >= 5
  const description = draft.description.trim()
  if (!draft.hazardTypeId || !draft.category) return null
  if (description.length < LIMITS.description.min || description.length > LIMITS.description.max) return null
  if (!hasGps && !manualOk) return null
  if (draft.photo && draft.photo.size > LIMITS.photoBytes) return null
  return {
    clientRef: draft.id,
    hazardTypeId: draft.hazardTypeId,
    category: draft.category,
    description,
    latitude: hasGps ? draft.latitude : null,
    longitude: hasGps ? draft.longitude : null,
    isManualLocation: !hasGps,
    manualLocationText: hasGps ? null : draft.manualText.trim(),
    districtId: draft.districtId,
    capturedAt: draft.createdAt,
    photo: draft.photo as File | null,
  }
}
