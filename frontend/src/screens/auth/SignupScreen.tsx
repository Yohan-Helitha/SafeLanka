import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { PasswordField } from '@/components/auth/PasswordField'
import { ApiErrorNotice } from '@/components/domain'
import { Button, Field, Input, Segmented, Select } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { EMAIL_PATTERN, MESSAGES, NIC_PATTERN, PASSWORD_PATTERN, isPhone } from '@/constants/validation'
import { useAuth } from '@/context/AuthContext'
import { useDocumentTitle } from '@/hooks/shared'
import { useDistricts, useSignup } from '@/hooks/auth/useAuthActions'
import { isApiError } from '@/services'
import type { Language, VerifyState } from '@/types'
import { compact } from '@/utils/validation'

const LANGUAGES: { value: Language; label: string }[] = [
  { value: 'si', label: 'සිංහල' },
  { value: 'ta', label: 'தமிழ்' },
  { value: 'en', label: 'English' },
]

export function SignupScreen() {
  useDocumentTitle('Create an account')
  const { user, mode } = useAuth()
  const navigate = useNavigate()
  const districts = useDistricts()
  const signup = useSignup()

  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [showEmail, setShowEmail] = useState(false)
  const [nic, setNic] = useState('')
  const [homeAddress, setHomeAddress] = useState('')
  const [districtId, setDistrictId] = useState('')
  const [language, setLanguage] = useState<Language>('en')
  const [password, setPassword] = useState('')
  const [attempted, setAttempted] = useState(false)

  if (mode === 'demo') return <Navigate to={paths.landing} replace />
  if (user) return <Navigate to={homeFor(user.role)} replace />

  const errors = compact({
    fullName: fullName.trim().length >= 2 && fullName.trim().length <= 120 ? null : 'Your name must be 2 to 120 characters.',
    phone: isPhone(phone) ? null : MESSAGES.phone,
    email: !showEmail || !email.trim() || EMAIL_PATTERN.test(email.trim()) ? null : MESSAGES.email,
    nic: NIC_PATTERN.test(nic.trim()) ? null : MESSAGES.nic,
    homeAddress: homeAddress.trim().length >= 5 && homeAddress.trim().length <= 200 ? null : 'The address must be 5 to 200 characters.',
    districtId: districtId ? null : 'Choose your district.',
    password: PASSWORD_PATTERN.test(password) ? null : MESSAGES.password,
  })

  // Server-side findings (duplicates, rules the browser cannot check) land on the same fields.
  const serverErrors: Record<string, string> = {}
  if (isApiError(signup.error)) {
    Object.assign(serverErrors, signup.error.fieldErrors)
    const conflictField = signup.error.code === 'CONFLICT' ? (signup.error.details?.field as string | undefined) : undefined
    if (conflictField) serverErrors[conflictField] = signup.error.message
  }
  const show = (key: string): string | null =>
    (attempted ? (errors[key] as string | undefined) : undefined) ?? serverErrors[key] ?? null

  const submit = () => {
    setAttempted(true)
    if (Object.keys(errors).length) return
    signup.mutate(
      {
        fullName: fullName.trim(),
        phone: phone.trim(),
        email: showEmail && email.trim() ? email.trim() : undefined,
        nic: nic.trim(),
        homeAddress: homeAddress.trim(),
        districtId,
        preferredLanguage: language,
        password,
      },
      {
        onSuccess: (result) => {
          const state: VerifyState = { phone: phone.trim(), maskedPhone: result.phone, otpExpiresAt: result.otpExpiresAt, devCode: result.devCode }
          navigate(paths.auth.verify, { state })
        },
      },
    )
  }

  return (
    <AuthShell
      title="Create an account"
      subtitle="Citizens register with a mobile number. We send a code by SMS to confirm it."
      footer={
        <>
          Already registered?{' '}
          <Link to={paths.auth.login} className="font-medium text-signal hover:underline">
            Log in
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
        <Field label="Full name" required error={show('fullName')}>
          {(p) => <Input {...p} autoComplete="name" value={fullName} onChange={(e) => setFullName(e.target.value)} />}
        </Field>
        <Field label="Mobile number" required error={show('phone')} hint="We will text a 6-digit code to this number.">
          {(p) => <Input {...p} type="tel" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="077 123 4567" />}
        </Field>
        {showEmail ? (
          <Field label="Email (optional)" error={show('email')}>
            {(p) => <Input {...p} type="email" inputMode="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" />}
          </Field>
        ) : (
          <Button variant="ghost" size="sm" icon={<Plus className="size-4" aria-hidden />} onClick={() => setShowEmail(true)}>
            Add email (optional)
          </Button>
        )}
        <Field label="NIC number" required error={show('nic')} hint="Old format 199012345V, or the 12-digit number.">
          {(p) => <Input {...p} autoCapitalize="characters" value={nic} onChange={(e) => setNic(e.target.value)} />}
        </Field>
        <Field label="Home address" required error={show('homeAddress')}>
          {(p) => <Input {...p} autoComplete="street-address" value={homeAddress} onChange={(e) => setHomeAddress(e.target.value)} placeholder="45 Station Road, Kolonnawa" />}
        </Field>
        <Field label="District" required error={show('districtId')} hint="We use your district to find the river and area that affect you.">
          {(p) => (
            <Select {...p} value={districtId} onChange={(e) => setDistrictId(e.target.value)}>
              <option value="">Choose your district</option>
              {(districts.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Segmented<Language> legend="Preferred language" columns={3} value={language} onChange={setLanguage} options={LANGUAGES} />
        <PasswordField label="Password" value={password} onChange={setPassword} error={show('password')} hint={MESSAGES.password} autoComplete="new-password" />
        <ApiErrorNotice error={signup.error && !Object.keys(serverErrors).length ? signup.error : null} />
        {Object.keys(serverErrors).length > 0 && signup.error && (
          <p className="text-sm text-danger" role="alert">
            {isApiError(signup.error) && signup.error.code === 'CONFLICT' ? (
              <>
                {signup.error.message}{' '}
                <Link to={paths.auth.login} className="font-medium underline">
                  Log in instead
                </Link>
              </>
            ) : (
              'Fix the highlighted fields and try again.'
            )}
          </p>
        )}
        <Button type="submit" size="lg" block loading={signup.isPending}>
          Create account
        </Button>
      </form>
    </AuthShell>
  )
}
