import { env } from '@/constants/env'
import { AccountMenu } from './AccountMenu'
import { RoleSwitcher } from './RoleSwitcher'

/** Account menu with Log out; in demo mode the role switcher takes its place. */
export function UserMenu({ compact = false }: { compact?: boolean }) {
  return env.authMode === 'demo' ? <RoleSwitcher compact={compact} /> : <AccountMenu compact={compact} />
}
