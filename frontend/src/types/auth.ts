import type { Id } from './common'
import type { AppUser } from './reference'

/** The signed-in person, as the app needs them. A full AppUser also fits (demo mode). */
export type SessionUser = Pick<AppUser, 'id' | 'fullName' | 'role' | 'districtId' | 'riverBasinId' | 'rescueTeamId'>

export type IdentifierType = 'PHONE' | 'EMAIL'
export type Language = 'en' | 'si' | 'ta'

export interface LoginInput {
  identifierType: IdentifierType
  identifier: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  /** Seconds until the access token expires. */
  expiresIn: number
  user: SessionUser
}

export interface SignupInput {
  fullName: string
  phone: string
  email?: string
  nic: string
  homeAddress: string
  districtId: Id
  preferredLanguage: Language
  password: string
}

export interface SignupResult {
  userId: Id
  /** Masked, for display: +94 77 *** 4567. */
  phone: string
  otpExpiresAt: string
  /** Only present where the SMS gateway is mocked (dev profile). */
  devCode?: string
}

export interface OtpResult {
  otpExpiresAt: string
  devCode?: string
}

/** Router state handed from sign-up or login to the verify screen. */
export interface VerifyState {
  /** What the person typed, sent back with the code. */
  phone: string
  maskedPhone?: string
  otpExpiresAt?: string
  devCode?: string
}
