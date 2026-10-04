import { MessageSquareText } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { ApiErrorNotice } from '@/components/domain'
import { Button, Field, Input } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { isPhone, MESSAGES } from '@/constants/validation'
import { useAuth } from '@/context/AuthContext'
import { useDocumentTitle } from '@/hooks/shared'
import { useResendCode, useVerifyPhone } from '@/hooks/auth/useAuthActions'
import type { VerifyState } from '@/types'
import { formatTime } from '@/utils/format'

const RESEND_SECONDS = 60

export function VerifyPhoneScreen() {
  useDocumentTitle('Verify your mobile number')
  const { user, mode } = useAuth()
  const state = useLocation().state as VerifyState | null
  const verify = useVerifyPhone()
  const resend = useResendCode()

  const [phone, setPhone] = useState(state?.phone ?? '')
  const [code, setCode] = useState('')
  const [devCode, setDevCode] = useState(state?.devCode)
  const [expiresAt, setExpiresAt] = useState(state?.otpExpiresAt)
  // A code was just sent when we arrive with state, so the wait starts now.
  const [seconds, setSeconds] = useState(state ? RESEND_SECONDS : 0)

  useEffect(() => {
    if (seconds <= 0) return
    const t = setTimeout(() => setSeconds((s) => s - 1), 1000)
    return () => clearTimeout(t)
  }, [seconds])

  if (mode === 'demo') return <Navigate to={paths.landing} replace />
  if (user) return <Navigate to={homeFor(user.role)} replace />

  const phoneKnown = Boolean(state?.phone)
  const phoneError = phone && !isPhone(phone) && !phone.startsWith('+') ? MESSAGES.phone : null
  const ready = code.length === 6 && phone.trim().length > 0

  const requestNewCode = () => {
    verify.reset()
    resend.mutate(phone.trim(), {
      onSuccess: (r) => {
        setDevCode(r.devCode)
        setExpiresAt(r.otpExpiresAt)
        setCode('')
        setSeconds(RESEND_SECONDS)
      },
    })
  }

  return (
    <AuthShell
      title="Verify your mobile number"
      subtitle={
        state?.maskedPhone ? (
          <>We sent a 6-digit code to {state.maskedPhone}.</>
        ) : phoneKnown ? (
          <>We sent a 6-digit code to your mobile number.</>
        ) : (
          'Enter your mobile number and the 6-digit code we sent you.'
        )
      }
      footer={
        <Link to={paths.auth.login} className="font-medium text-signal hover:underline">
          Back to log in
        </Link>
      }
    >
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          if (ready) verify.mutate({ phone: phone.trim(), code })
        }}
        className="space-y-4"
      >
        {!phoneKnown && (
          <Field label="Mobile number" required error={phoneError}>
            {(p) => <Input {...p} type="tel" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="077 123 4567" />}
          </Field>
        )}
        <Field label="Verification code" required hint={expiresAt ? `The code expires at ${formatTime(expiresAt)}.` : undefined}>
          {(p) => (
            <Input
              {...p}
              inputMode="numeric"
              autoComplete="one-time-code"
              autoFocus
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="123456"
              className="tabular text-center font-display text-2xl tracking-[0.4em]"
            />
          )}
        </Field>

        {devCode && (
          <p className="flex items-start gap-2 rounded-control border border-caution/50 bg-caution/10 p-3 text-sm text-ink">
            <MessageSquareText className="mt-0.5 size-4 shrink-0 text-caution" aria-hidden />
            <span>
              Demo code: <span className="tabular font-semibold">{devCode}</span>. SMS is simulated, so the code is shown here.
            </span>
          </p>
        )}

        <ApiErrorNotice error={verify.error ?? resend.error} />
        <Button type="submit" size="lg" block loading={verify.isPending} disabled={!ready}>
          Verify and continue
        </Button>
        <Button variant="secondary" block disabled={seconds > 0 || !phone.trim()} loading={resend.isPending} onClick={requestNewCode}>
          {seconds > 0 ? `Request a new code in ${seconds} s` : 'Request a new code'}
        </Button>
      </form>
    </AuthShell>
  )
}
