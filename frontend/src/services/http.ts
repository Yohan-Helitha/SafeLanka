import { env } from '@/constants/env'
import type { Page } from '@/types'
import { ApiError } from './ApiError'
import type { ApiErrorCode } from './ApiError'
import { connectivity } from './offline/connectivity'
import { getActingUserId } from './session'

type Query = Record<string, string | number | boolean | undefined | null | (string | number)[]>

function buildUrl(path: string, query?: Query): string {
  const url = new URL(env.apiBaseUrl.replace(/\/$/, '') + path, window.location.origin)
  Object.entries(query ?? {}).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    if (Array.isArray(v)) v.forEach((x) => url.searchParams.append(k, String(x)))
    else url.searchParams.set(k, String(v))
  })
  return url.toString()
}

async function send(path: string, init: RequestInit & { query?: Query } = {}): Promise<Response> {
  if (connectivity.isOffline()) throw ApiError.offline()
  const headers = new Headers(init.headers)
  const user = getActingUserId()
  if (user) headers.set('X-Acting-User', user)
  let res: Response
  try {
    res = await fetch(buildUrl(path, init.query), { ...init, headers })
  } catch {
    throw ApiError.offline()
  }
  if (!res.ok) {
    let body: { error?: { code?: ApiErrorCode; message?: string; details?: Record<string, unknown> } } = {}
    try {
      body = await res.json()
    } catch {
      /* empty body */
    }
    throw new ApiError(
      res.status,
      body.error?.code ?? 'INTERNAL_ERROR',
      body.error?.message ?? 'Something went wrong on our side. Try again in a moment.',
      body.error?.details,
    )
  }
  return res
}

async function json<T>(res: Response): Promise<T> {
  if (res.status === 204) return null as T
  const body = await res.json()
  return (body && typeof body === 'object' && 'data' in body ? body.data : body) as T
}

function jsonInit(method: string, body?: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  }
}

export const http = {
  async get<T>(path: string, query?: Query): Promise<T> {
    return json<T>(await send(path, { query }))
  },
  /** GET that returns a paginated envelope as {items, page, size, totalElements, totalPages}. */
  async page<T>(path: string, query?: Query): Promise<Page<T>> {
    const res = await send(path, { query })
    const body = await res.json()
    const meta = body.meta ?? {}
    return {
      items: body.data as T[],
      page: meta.page ?? 0,
      size: meta.size ?? (body.data as T[]).length,
      totalElements: meta.totalElements ?? (body.data as T[]).length,
      totalPages: meta.totalPages ?? 1,
    }
  },
  async post<T>(path: string, body?: unknown): Promise<T> {
    return json<T>(await send(path, jsonInit('POST', body)))
  },
  async put<T>(path: string, body?: unknown): Promise<T> {
    return json<T>(await send(path, jsonInit('PUT', body)))
  },
  async patch<T>(path: string, body?: unknown): Promise<T> {
    return json<T>(await send(path, jsonInit('PATCH', body)))
  },
  async postForm<T>(path: string, form: FormData): Promise<T> {
    return json<T>(await send(path, { method: 'POST', body: form }))
  },
  async blob(path: string, query?: Query): Promise<Blob> {
    return (await send(path, { query })).blob()
  },
}
