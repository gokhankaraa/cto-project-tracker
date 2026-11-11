import { Alert, Button, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Navigate, useLocation } from 'react-router'

import { useLogin } from '../api/auth'
import { ApiRequestError, errorMessage } from '../api/client'
import type { OverallStatus } from '../api/types'
import type { LoginLocationState } from '../auth/RequireAuth'
import { saveSession, sessionEndReason } from '../auth/session'
import { useSession } from '../auth/useSession'
import { BrandMark } from '../components/BrandMark'
import { OVERALL_STATUS_LABELS } from '../lib/labels'
import { STATUS_SWATCHES } from '../lib/statusSwatches'
import classes from './LoginPage.module.css'

interface FormValues {
  email: string
  password: string
}

/**
 * Giristen sonra donulecek adres. Yalnizca uygulama ici yollar kabul edilir: '//baska-site' gibi bir
 * deger tarayicida baska bir siteye gider.
 */
function returnPath(state: unknown): string {
  const from = (state as LoginLocationState | null)?.from
  if (typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.startsWith('/login')) {
    return from
  }
  return '/'
}

/** /login. Oturum acilinca (ya da zaten aciksa) istenen sayfaya yonlendirir. */
export function LoginPage() {
  const session = useSession()
  const location = useLocation()
  const queryClient = useQueryClient()
  const login = useLogin()
  // Not, sayfa acildigi andaki duruma gore bir kez belirlenir.
  const [endReason] = useState(sessionEndReason)

  const form = useForm<FormValues>({
    initialValues: { email: '', password: '' },
    validate: {
      email: (value) => (value.trim() ? null : 'E-posta zorunludur.'),
      password: (value) => (value ? null : 'Parola zorunludur.'),
    },
  })

  if (session) return <Navigate to={returnPath(location.state)} replace />

  const submit = form.onSubmit((values) =>
    login.mutate(
      { email: values.email.trim(), password: values.password },
      {
        onSuccess: (response) => {
          // Onceki oturumun (baska bir rolun) onbellege alinmis verisi yeni kullaniciya gosterilmez.
          queryClient.clear()
          // Oturum degisince bu sayfa yeniden render olur ve yukaridaki Navigate yonlendirir.
          saveSession(response)
        },
        onError: (error) => {
          // Sira onemli: alan degeri degisince Mantine o alanin hatasini siler; parola once temizlenir.
          form.setFieldValue('password', '')
          if (error instanceof ApiRequestError) form.setErrors(error.fieldErrorMap())
        },
      },
    ),
  )

  const fieldErrorsOnly = login.error instanceof ApiRequestError && login.error.fieldErrors.length > 0
  const showEndNote = endReason === 'expired' && login.isIdle

  return (
    <div className={classes.page}>
      <Showcase />
      <main className={classes.formSide}>
        <form className={classes.form} onSubmit={submit} noValidate>
          <Stack gap="lg">
            <div className={classes.brand}>
              <BrandMark size={44} />
              <div>
                <Title order={1} fz={26} lh={1.2}>
                  CTO Takip Sistemi
                </Title>
                <Text c="dimmed" size="sm">
                  Devam etmek için giriş yapın.
                </Text>
              </div>
            </div>
            {showEndNote && (
              <Alert color="yellow" title="Oturum sona erdi">
                Güvenliğiniz için oturumunuz kapandı. Lütfen tekrar giriş yapın.
              </Alert>
            )}
            {login.isError && !fieldErrorsOnly && (
              <Alert color="red" title="Giriş yapılamadı">
                {errorMessage(login.error)}
              </Alert>
            )}
            <Stack gap="md">
              <TextInput
                label="E-posta"
                type="email"
                autoComplete="username"
                size="md"
                withAsterisk
                {...form.getInputProps('email')}
              />
              <PasswordInput
                label="Parola"
                autoComplete="current-password"
                size="md"
                withAsterisk
                {...form.getInputProps('password')}
              />
            </Stack>
            <Button type="submit" loading={login.isPending} size="md" fullWidth>
              Giriş yap
            </Button>
          </Stack>
        </form>
      </main>
    </div>
  )
}

const STATUS_BY_CODE: Record<string, OverallStatus> = { Y: 'YOLUNDA', R: 'RISKLI', G: 'GECIKMELI', B: 'BLOKE' }

// Ornek dort projenin on iki haftasi (Y yolunda, R riskli, G gecikmeli, B bloke). Gercek veri degil; yalnizca gorsel.
const SAMPLE_WEEKS = ['YYYYYYYYRYYY', 'YYRRGGBBYYYY', 'RRYYYYYYYYYY', 'YYYYYYRRGGRR']

/** Genis ekrandaki sol panel: uygulamanin ne yaptigi ve haftalik durum izgarasinin ornegi. */
function Showcase() {
  return (
    <aside className={classes.showcase}>
      <div>
        <p className={classes.tagline}>Haftalık raporlar, riskler ve kararlar tek ekranda.</p>
        <p className={classes.taglineNote}>
          Proje yöneticileri her hafta durumu yazar; CTO tüm portföyü, geciken işleri ve bekleyen kararları bir bakışta
          görür.
        </p>
      </div>
      <div className={classes.weeks} aria-hidden="true">
        <div className={classes.weeksTitle}>Son 12 hafta</div>
        <div className={classes.weeksGrid}>
          {SAMPLE_WEEKS.flatMap((row, r) =>
            [...row].map((code, w) => (
              <span
                key={`${r}-${w}`}
                className={classes.cell}
                style={{ background: STATUS_SWATCHES[STATUS_BY_CODE[code]] }}
              />
            )),
          )}
        </div>
        <div className={classes.legend}>
          {(Object.keys(STATUS_SWATCHES) as OverallStatus[]).map((status) => (
            <span key={status} className={classes.legendItem}>
              <span className={classes.swatch} style={{ background: STATUS_SWATCHES[status] }} />
              {OVERALL_STATUS_LABELS[status].label}
            </span>
          ))}
        </div>
      </div>
    </aside>
  )
}
