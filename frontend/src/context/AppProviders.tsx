import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { isApiError } from '@/services'
import { AuthProvider } from './AuthContext'
import { OutboxProvider } from './OutboxContext'
import { ToastProvider } from './ToastContext'

function makeClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15_000,
        retry: (count, error) => !(isApiError(error) && error.status >= 400 && error.status < 500) && count < 2,
        refetchOnWindowFocus: false,
      },
    },
  })
}

export function AppProviders({ children }: { children: ReactNode }) {
  const [client] = useState(makeClient)
  return (
    <QueryClientProvider client={client}>
      <ToastProvider>
        <AuthProvider>
          <OutboxProvider>{children}</OutboxProvider>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
