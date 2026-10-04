import { useMutation, useQuery } from '@tanstack/react-query'
import { useAuth } from '@/context/AuthContext'
import { api } from '@/services'
import type { LoginInput, SignupInput } from '@/types'

/** Log in; the service layer keeps the token, this records the person in app state. */
export function useLogin() {
  const { applyLogin } = useAuth()
  return useMutation({ mutationFn: (input: LoginInput) => api.auth.login(input), onSuccess: applyLogin })
}

export function useSignup() {
  return useMutation({ mutationFn: (input: SignupInput) => api.auth.signup(input) })
}

/** Checking the SMS code activates the account and signs the person in. */
export function useVerifyPhone() {
  const { applyLogin } = useAuth()
  return useMutation({
    mutationFn: (v: { phone: string; code: string }) => api.auth.verifyPhone(v.phone, v.code),
    onSuccess: applyLogin,
  })
}

export function useResendCode() {
  return useMutation({ mutationFn: (phone: string) => api.auth.resendCode(phone) })
}

/** Districts for the sign-up form. Public, so it works before anyone is signed in. */
export function useDistricts() {
  return useQuery({ queryKey: ['reference', 'districts'], queryFn: () => api.reference.districts(), staleTime: 10 * 60_000 })
}
