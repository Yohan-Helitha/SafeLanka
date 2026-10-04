import type { AppUser, Role } from '@/types'
import { fail } from './mockCall'
import { db } from './db'
import { getActingUserId } from '../session'

/** The acting user from the X-Acting-User equivalent. */
export function actor(): AppUser {
  const user = db.users.find((u) => u.id === getActingUserId())
  if (!user) fail('FORBIDDEN_ROLE', 'Choose who you are acting as first.')
  return user
}

export function requireRole(...roles: Role[]): AppUser {
  const user = actor()
  if (!roles.includes(user.role)) fail('FORBIDDEN_ROLE', 'Your role cannot do this.')
  return user
}
