import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { POLL } from '@/constants/app'
import { api, isApiError } from '@/services'
import { connectivity } from '@/services/offline/connectivity'
import { outboxStore } from '@/services/offline/outbox'
import type { OutboxItem, OutboxPayload } from '@/services/offline/outbox'
import { plural } from '@/utils/format'
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

export function OutboxProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [items, setItems] = useState<OutboxItem[]>([])
  const [flushing, setFlushing] = useState(false)
  const busy = useRef(false)

  const reload = useCallback(async () => setItems(await outboxStore.all()), [])

  useEffect(() => {
    void reload()
  }, [reload])

  const enqueue = useCallback(
    async (clientRef: string, payload: OutboxPayload, label: string) => {
      await outboxStore.put({
        clientRef,
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
    [reload],
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
        try {
          await send(item.payload)
          await outboxStore.remove(item.clientRef)
          sent += 1
        } catch (e) {
          if (isApiError(e) && e.isOffline) break
          const message = isApiError(e) ? e.message : 'Could not send this item.'
          // Server rejected it (4xx): keep it, but stop retrying until the person looks at it.
          await outboxStore.put({
            ...item,
            attempts: item.attempts + 1,
            state: isApiError(e) && e.status >= 400 && e.status < 500 ? 'NEEDS_ATTENTION' : 'PENDING',
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
  }, [queryClient, reload, toast])

  useEffect(() => {
    const unsubscribe = connectivity.subscribe(() => {
      if (!connectivity.isOffline()) void flush()
    })
    const timer = setInterval(() => void flush(), POLL.outbox)
    return () => {
      unsubscribe()
      clearInterval(timer)
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
