import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { POLL } from '@/constants/app'
import { api, isApiError } from '@/services'
import { connectivity } from '@/services/offline/connectivity'
import { outboxStore } from '@/services/offline/outbox'
import type { OutboxItem, OutboxPayload } from '@/services/offline/outbox'
import { plural } from '@/utils/format'
import { useAuth } from './AuthContext'
import { useToast } from './ToastContext'

interface OutboxState {
  items: OutboxItem[]
  pendingCount: number
  attentionCount: number
  enqueue: (clientRef: string, payload: OutboxPayload, label: string) => Promise<void>
  discard: (clientRef: string) => Promise<void>
  flush: () => Promise<void>
  flushing: boolean
}

const OutboxContext = createContext<OutboxState | null>(null)

async function send(payload: OutboxPayload): Promise<void> {
  switch (payload.kind) {
    case 'REPORT':
      await api.reports.submit(payload.input)
      return
    case 'TEAM_STATUS':
      await api.response.updateTeamStatus(payload.teamId, payload.update)
      return
    case 'DISTRIBUTION':
      await api.response.recordDistribution(payload.allocationId, payload.input)
      return
  }
}

/** A 4xx means the item itself is wrong, except for answers that only mean "not now" (expired login, slow, busy). */
function isFinalRefusal(status: number): boolean {
  const tryAgain = [401, 408, 425, 429]
  return status >= 400 && status < 500 && !tryAgain.includes(status)
}

export function OutboxProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const { user } = useAuth()
  const ownerId = user?.id
  const [all, setAll] = useState<OutboxItem[]>([])
  const [flushing, setFlushing] = useState(false)
  const busy = useRef(false)

  const reload = useCallback(async () => setAll(await outboxStore.all()), [])
  // Only the signed-in person's items are shown and sent.
  const items = useMemo(() => all.filter((i) => !i.ownerId || i.ownerId === ownerId), [all, ownerId])

  useEffect(() => {
    void reload()
  }, [reload])

  const enqueue = useCallback(
    async (clientRef: string, payload: OutboxPayload, label: string) => {
      await outboxStore.put({
        clientRef,
        ownerId,
        kind: payload.kind,
        payload,
        label,
        createdAt: new Date().toISOString(),
        attempts: 0,
        state: 'PENDING',
        lastError: null,
      })
      await reload()
    },
    [reload, ownerId],
  )

  const discard = useCallback(
    async (clientRef: string) => {
      await outboxStore.remove(clientRef)
      await reload()
    },
    [reload],
  )

  const flush = useCallback(async () => {
    if (busy.current || connectivity.isOffline()) return
    busy.current = true
    setFlushing(true)
    let sent = 0
    try {
      for (const item of await outboxStore.all()) {
        if (item.state === 'NEEDS_ATTENTION') continue
        if (item.ownerId && item.ownerId !== ownerId) continue
        try {
          await send(item.payload)
          await outboxStore.remove(item.clientRef)
          sent += 1
        } catch (e) {
          if (isApiError(e) && e.isOffline) break
          const message = isApiError(e) ? e.message : 'Could not send this item.'
          // The server refused it (4xx other than "try again"): keep it, but stop retrying until the person looks at it.
          await outboxStore.put({
            ...item,
            attempts: item.attempts + 1,
            state: isApiError(e) && isFinalRefusal(e.status) ? 'NEEDS_ATTENTION' : 'PENDING',
            lastError: message,
          })
        }
      }
    } finally {
      busy.current = false
      setFlushing(false)
      await reload()
    }
    if (sent > 0) {
      toast(`${plural(sent, 'saved item')} ${sent === 1 ? 'was' : 'were'} sent`)
      await queryClient.invalidateQueries()
    }
  }, [queryClient, reload, toast, ownerId])

  // What is waiting, readable from timers and events without re-creating them.
  const waiting = useRef(false)
  useEffect(() => {
    waiting.current = items.some((i) => i.state === 'PENDING')
  }, [items])

  useEffect(() => {
    // The browser says "online" a moment before the network works, so a send right then can fail;
    // the timer below keeps trying until the queue is empty.
    const tryNow = () => {
      if (!connectivity.isOffline()) void flush()
    }
    const unsubscribe = connectivity.subscribe(tryNow)
    const timer = setInterval(() => {
      if (waiting.current) tryNow()
    }, POLL.outbox)
    const onVisible = () => {
      if (document.visibilityState === 'visible') tryNow()
    }
    window.addEventListener('focus', tryNow)
    document.addEventListener('visibilitychange', onVisible)
    tryNow()
    return () => {
      unsubscribe()
      clearInterval(timer)
      window.removeEventListener('focus', tryNow)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [flush])

  const value = useMemo<OutboxState>(
    () => ({
      items,
      pendingCount: items.filter((i) => i.state === 'PENDING').length,
      attentionCount: items.filter((i) => i.state === 'NEEDS_ATTENTION').length,
      enqueue,
      discard,
      flush,
      flushing,
    }),
    [items, enqueue, discard, flush, flushing],
  )

  return <OutboxContext.Provider value={value}>{children}</OutboxContext.Provider>
}

export function useOutbox(): OutboxState {
  const ctx = useContext(OutboxContext)
  if (!ctx) throw new Error('useOutbox must be used inside OutboxProvider')
  return ctx
}
