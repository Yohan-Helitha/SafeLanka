import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { PasswordField } from '@/components/auth/PasswordField'
import { ApiErrorNotice } from '@/components/domain'
import { Button, Field, Input } from '@/components/ui'
import { env } from '@/constants/env'
import { homeFor } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { EMAIL_PATTERN, MESSAGES, isPhone } from '@/constants/validation'
import { useAuth } from '@/context/AuthContext'
import { useDocumentTitle } from '@/hooks/shared'
import { useLogin } from '@/hooks/auth/useAuthActions'
import { isApiError } from '@/services'
import type { IdentifierType, VerifyState } from '@/types'

export function LoginScreen() {
  useDocumentTitle('Log in')
  const { user, mode, loading } = useAuth()
  const navigate = useNavigate()
  const login = useLogin()

  const [type, setType] = useState<IdentifierType>('PHONE')
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [attempted, setAttempted] = useState(false)

  if (mode === 'demo') return <Navigate to={paths.landing} replace />
  if (user) return <Navigate to={homeFor(user.role)} replace />

  const phone = type === 'PHONE'
  const identifierError = !identifier.trim()
    ? phone ? 'Enter your mobile number.' : 'Enter your email.'
    : phone ? (isPhone(identifier) ? null : MESSAGES.phone) : EMAIL_PATTERN.test(identifier.trim()) ? null : MESSAGES.email
  const passwordError = password ? null : 'Enter your password.'

  const switchType = () => {
    setType(phone ? 'EMAIL' : 'PHONE')
    setIdentifier('')
    setAttempted(false)
    login.reset()
  }

  const submit = () => {
    setAttempted(true)
    if (identifierError || passwordError) return
    login.mutate(
      { identifierType: type, identifier: identifier.trim(), password },
      {
        onError: (e) => {
          // A citizen who never verified their number gets a fresh code and is taken to the code screen.
          if (isApiError(e) && e.code === 'PHONE_NOT_VERIFIED') {
            const d = e.details ?? {}
            const state: VerifyState = {
              phone: String(d.phone ?? identifier),
              otpExpiresAt: d.otpExpiresAt as string | undefined,
              devCode: d.devCode as string | undefined,
            }
            navigate(paths.auth.verify, { state })
          }
        },
      },
    )
  }

  return (
    <AuthShell
      title="Log in"
      subtitle="Use your mobile number or email and your password."
      footer={
        <>
          New here?{' '}
          <Link to={paths.auth.signup} className="font-medium text-signal hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
        className="space-y-4"
      >
        <div>
          <Field
            label={phone ? 'Mobile number' : 'Email'}
            required
            error={attempted ? identifierError : null}
            hint={phone ? 'For example 077 123 4567' : undefined}
          >
            {(p) => (
              <Input
                {...p}
                type={phone ? 'tel' : 'email'}
                inputMode={phone ? 'tel' : 'email'}
                autoComplete="username"
                autoFocus
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder={phone ? '077 123 4567' : 'name@example.com'}
              />
            )}
          </Field>
          <button type="button" onClick={switchType} className="mt-1.5 text-sm font-medium text-signal hover:underline">
            {phone ? 'Use email instead' : 'Use mobile number instead'}
          </button>
        </div>
        <PasswordField value={password} onChange={setPassword} error={attempted ? passwordError : null} autoComplete="current-password" />
        <ApiErrorNotice error={login.error} />
        <Button type="submit" size="lg" block loading={login.isPending || loading}>
          Log in
        </Button>
      </form>

      {env.useMocks && (
        <p className="mt-4 rounded-control border border-line bg-raised p-3 text-xs text-muted">
          Demo accounts: every seeded person uses the password <span className="font-medium text-ink">Demo@1234</span>. Try
          nimal.perera@dmc.lk (DMC officer), kasun.jayawardena@dmc.lk (district officer), or the mobile number 077 100 0004 (citizen).
        </p>
      )}
    </AuthShell>
  )
}
