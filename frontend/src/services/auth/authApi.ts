import type { LoginInput, LoginResponse, OtpResult, SessionUser, SignupInput, SignupResult } from '@/types'

/**
 * Authentication. Methods that return a LoginResponse also establish the session (the access
 * token is kept in memory by the service layer), so callers only update their own state.
 */
export interface AuthApi {
  signup(input: SignupInput): Promise<SignupResult>
  verifyPhone(phone: string, code: string): Promise<LoginResponse>
  resendCode(phone: string): Promise<OtpResult>
  login(input: LoginInput): Promise<LoginResponse>
  /** Renews the session from the refresh cookie; rejects with UNAUTHENTICATED when there is none. */
  refresh(): Promise<LoginResponse>
  logout(): Promise<void>
  me(): Promise<SessionUser>
}
