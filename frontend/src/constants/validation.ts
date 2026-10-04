/** Input rules for sign-up and login. They mirror the backend, which stays the final authority. */
export const PHONE_PATTERN = /^(?:\+94|0)?7\d{8}$/
export const NIC_PATTERN = /^(?:\d{9}[VvXx]|\d{12})$/
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const MESSAGES = {
  phone: 'Enter a Sri Lankan mobile number, for example 077 123 4567.',
  nic: 'Enter a valid NIC: 9 digits and V, or 12 digits.',
  password: 'Use 8 to 64 characters with at least one letter and one number.',
  email: 'Enter a valid email address.',
} as const

/** Strips spaces and dashes, so "077 123-4567" is accepted. */
export function compactPhone(input: string): string {
  return input.replace(/[\s-]/g, '')
}

export function isPhone(input: string): boolean {
  return PHONE_PATTERN.test(compactPhone(input))
}

/** E.164 form (+947XXXXXXXX), or null when it is not a Sri Lankan mobile number. */
export function toE164(input: string): string | null {
  const compact = compactPhone(input)
  return PHONE_PATTERN.test(compact) ? `+94${compact.slice(-9)}` : null
}
