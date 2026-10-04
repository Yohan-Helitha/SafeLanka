import { RouterProvider } from 'react-router-dom'
import { AppProviders } from '@/context/AppProviders'
import { router } from '@/navigation/router'

export default function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  )
}
