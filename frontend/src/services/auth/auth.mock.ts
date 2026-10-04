import type { AppUser, LoginResponse, SessionUser, SignupInput } from '@/types'
import { newId, seedId } from '../mocks/ids'
import { db } from '../mocks/db'
import { fail, mockCall, validationFail } from '../mocks/mockCall'
import { getActingUserId, setAccessToken, setActingUserId } from '../session'
import type { AuthApi } from './authApi'

/** In-browser stand-in for the backend rules: same validation, lockout and SMS-code behaviour. */

const DEMO_PASSWORD = 'Demo@1234'
const MAX_FAILURES = 5
const LOCK_MS = 15 * 60_000
const OTP_TTL_MS = 5 * 60_000
const OTP_COOLDOWN_MS = 60_000
const OTP_MAX_ATTEMPTS = 3

const PHONE_RE = /^(?:\+94|0)?7\d{8}$/
const NIC_RE = /^(?:\d{9}[VvXx]|\d{12})$/
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/

interface Account {
  userId: string
  phone: string
  email: string | null
  nic: string | null
  password: string
  pending: boolean
  failures: number
  lockedUntil: number
}

interface PendingCode {
  code: string
  expiresAt: number
  sentAt: number
  attempts: number
}

const SEED_EMAILS: Record<number, string> = {
  1: 'nimal.perera@dmc.lk',
  2: 'kasun.jayawardena@dmc.lk',
  3: 'sanduni.wickramasinghe@dmc.lk',
  4: 'ruwan.fernando@example.lk',
  5: 'tharindu.silva@example.lk',
  6: 'dilani.gunasekara@dmc.lk',
  7: 'asanka.bandara@dmc.lk',
  8: 'mahesh.kumara@dmc.lk',
  9: 'priya.shanmugam@example.lk',
}

const accounts: Account[] = Object.entries(SEED_EMAILS).map(([n, email]) => ({
  userId: seedId(6, Number(n)),
  phone: `+9477100000${n}`,
  email,
  nic: null,
  password: DEMO_PASSWORD,
  pending: false,
  failures: 0,
  lockedUntil: 0,
}))

const codes = new Map<string, PendingCode>()

function normalisePhone(input: string): string | null {
  const compact = input.replace(/[\s-]/g, '')
  return PHONE_RE.test(compact) ? `+94${compact.slice(-9)}` : null
}

function masked(e164: string): string {
  const d = e164.slice(3)
  return `+94 ${d.slice(0, 2)} *** ${d.slice(5)}`
}

function toSession(userId: string): SessionUser {
  const u = db.users.find((x) => x.id === userId)!
  return { id: u.id, fullName: u.fullName, role: u.role, districtId: u.districtId, riverBasinId: u.riverBasinId, rescueTeamId: u.rescueTeamId }
}

function loginFor(account: Account): LoginResponse {
  setAccessToken('mock-access-token')
  setActingUserId(account.userId) // the mock backend reads who is acting from here
  return { accessToken: 'mock-access-token', expiresIn: 900, user: toSession(account.userId) }
}

function sendCode(account: Account): { otpExpiresAt: string; devCode: string } {
  const now = Date.now()
  const previous = codes.get(account.phone)
  if (previous && now - previous.sentAt < OTP_COOLDOWN_MS) {
    const seconds = Math.ceil((OTP_COOLDOWN_MS - (now - previous.sentAt)) / 1000)
    fail('TOO_MANY_REQUESTS', `Wait ${seconds} seconds before asking for another code.`, { retryAfterSeconds: seconds })
  }
  const code = String(Math.floor(Math.random() * 1_000_000)).padStart(6, '0')
  codes.set(account.phone, { code, expiresAt: now + OTP_TTL_MS, sentAt: now, attempts: 0 })
  return { otpExpiresAt: new Date(now + OTP_TTL_MS).toISOString(), devCode: code }
}

function clockTime(ms: number): string {
  return new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Colombo', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(ms))
}

function checkSignup(input: SignupInput): Record<string, string> {
  const e: Record<string, string> = {}
  if (input.fullName.trim().length < 2 || input.fullName.trim().length > 120) e.fullName = 'Your name must be 2 to 120 characters.'
  if (!PHONE_RE.test(input.phone.replace(/[\s-]/g, ''))) e.phone = 'Enter a Sri Lankan mobile number, for example 077 123 4567.'
  if (input.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(input.email)) e.email = 'Enter a valid email address.'
  if (!NIC_RE.test(input.nic.trim())) e.nic = 'Enter a valid NIC: 9 digits and V, or 12 digits.'
  if (input.homeAddress.trim().length < 5 || input.homeAddress.trim().length > 200) e.homeAddress = 'The address must be 5 to 200 characters.'
  if (!db.districts.some((d) => d.id === input.districtId)) e.districtId = 'Choose your district.'
  if (!PASSWORD_RE.test(input.password)) e.password = 'Use 8 to 64 characters with at least one letter and one number.'
  return e
}

function findAccount(type: 'PHONE' | 'EMAIL', identifier: string): Account | undefined {
  if (type === 'PHONE') {
    const phone = normalisePhone(identifier)
    return phone ? accounts.find((a) => a.phone === phone) : undefined
  }
  const email = identifier.trim().toLowerCase()
  return accounts.find((a) => a.email === email)
}

function invalid(type: 'PHONE' | 'EMAIL'): never {
  fail('INVALID_CREDENTIALS', `The ${type === 'PHONE' ? 'mobile number' : 'email'} or password is incorrect.`)
}

export const authMock: AuthApi = {
  signup: (input) =>
    mockCall(() => {
      const errors = checkSignup(input)
      if (Object.keys(errors).length) validationFail(errors)
      const phone = normalisePhone(input.phone)!
      const email = input.email?.trim().toLowerCase() || null
      const nic = input.nic.trim().toUpperCase()
      if (accounts.some((a) => a.phone === phone)) fail('CONFLICT', 'That mobile number is already registered.', { field: 'phone' })
      if (email && accounts.some((a) => a.email === email)) fail('CONFLICT', 'That email is already registered.', { field: 'email' })
      if (accounts.some((a) => a.nic === nic)) fail('CONFLICT', 'That NIC is already registered.', { field: 'nic' })

      const basin = db.riverBasins.find((b) => b.districtIds.includes(input.districtId))
      const user: AppUser = {
        id: newId(),
        role: 'CITIZEN',
        fullName: input.fullName.trim(),
        districtId: input.districtId,
        riverBasinId: basin?.id ?? null,
        organisationId: null,
        rescueTeamId: null,
        preferredLanguage: input.preferredLanguage,
        named: false,
      }
      db.users.push(user)
      const account: Account = { userId: user.id, phone, email, nic, password: input.password, pending: true, failures: 0, lockedUntil: 0 }
      accounts.push(account)
      const otp = sendCode(account)
      return { userId: user.id, phone: masked(phone), ...otp }
    }),

  verifyPhone: (rawPhone, code) =>
    mockCall(() => {
      const phone = normalisePhone(rawPhone)
      const account = phone ? accounts.find((a) => a.phone === phone && a.pending) : undefined
      const pending = phone ? codes.get(phone) : undefined
      if (!account || !pending || pending.expiresAt <= Date.now() || pending.attempts >= OTP_MAX_ATTEMPTS) {
        fail('CODE_EXPIRED', 'Request a new code.')
      }
      if (pending.code !== code.trim()) {
        pending.attempts += 1
        const left = OTP_MAX_ATTEMPTS - pending.attempts
        fail('CODE_INVALID', left > 0 ? `That code is incorrect. ${left} ${left === 1 ? 'try' : 'tries'} left.` : 'That code is incorrect. Request a new code.', { attemptsLeft: left })
      }
      codes.delete(account.phone)
      account.pending = false
      return loginFor(account)
    }),

  resendCode: (rawPhone) =>
    mockCall(() => {
      const phone = normalisePhone(rawPhone)
      const account = phone ? accounts.find((a) => a.phone === phone && a.pending) : undefined
      if (!account) return { otpExpiresAt: new Date(Date.now() + OTP_TTL_MS).toISOString() }
      return sendCode(account)
    }),

  login: (input) =>
    mockCall(() => {
      const account = findAccount(input.identifierType, input.identifier)
      if (!account) invalid(input.identifierType)
      const now = Date.now()
      const locked = () => fail('ACCOUNT_LOCKED', `Too many attempts. Try again after ${clockTime(account.lockedUntil)}.`, { lockedUntil: new Date(account.lockedUntil).toISOString() })
      if (account.lockedUntil > now) locked()
      if (account.password !== input.password) {
        account.failures += 1
        if (account.failures >= MAX_FAILURES) {
          account.lockedUntil = now + LOCK_MS
          account.failures = 0
          locked()
        }
        invalid(input.identifierType)
      }
      account.failures = 0
      if (account.pending) {
        const otp = sendCodeOrReuse(account)
        fail('PHONE_NOT_VERIFIED', 'Verify your mobile number to continue. We sent you a new code.', { phone: account.phone, ...otp })
      }
      return loginFor(account)
    }),

  refresh: () =>
    mockCall(() => {
      const id = getActingUserId()
      const account = accounts.find((a) => a.userId === id && !a.pending)
      if (!account) fail('UNAUTHENTICATED', 'Log in to continue.')
      return loginFor(account)
    }),

  logout: () =>
    mockCall(() => {
      setAccessToken(null)
      setActingUserId(null)
    }),

  me: () =>
    mockCall(() => {
      const id = getActingUserId()
      if (!id || !accounts.some((a) => a.userId === id)) fail('UNAUTHENTICATED', 'Log in to continue.')
      return toSession(id)
    }),
}

/** At login an unverified person gets a code unless one was sent a moment ago. */
function sendCodeOrReuse(account: Account): { otpExpiresAt: string; devCode?: string } {
  const existing = codes.get(account.phone)
  if (existing && Date.now() - existing.sentAt < OTP_COOLDOWN_MS) {
    return { otpExpiresAt: new Date(existing.expiresAt).toISOString(), devCode: existing.code }
  }
  return sendCode(account)
}
