const raw = import.meta.env

export const env = {
  /** true = in-browser mock database, false = Spring Boot API. */
  useMocks: String(raw.VITE_USE_MOCKS ?? 'true') !== 'false',
  apiBaseUrl: String(raw.VITE_API_BASE_URL ?? 'http://localhost:8080/api'),
} as const
