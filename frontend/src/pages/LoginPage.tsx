import { Alert, Button, Center, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Navigate, useLocation } from 'react-router'

import { useLogin } from '../api/auth'
import { ApiRequestError, errorMessage } from '../api/client'
import type { LoginLocationState } from '../auth/RequireAuth'
import { saveSession, sessionEndReason } from '../auth/session'
import { useSession } from '../auth/useSession'

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
    <Center mih="100vh" p="md" bg="var(--mantine-color-body)">
      <Paper withBorder shadow="sm" p="xl" radius="md" w="100%" maw={400}>
        <form onSubmit={submit} noValidate>
          <Stack gap="md">
            <div>
              <Title order={2}>CTO Takip</Title>
              <Text c="dimmed" size="sm">
                Devam etmek için giriş yapın.
              </Text>
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
            <TextInput
              label="E-posta"
              type="email"
              autoComplete="username"
              withAsterisk
              {...form.getInputProps('email')}
            />
            <PasswordInput
              label="Parola"
              autoComplete="current-password"
              withAsterisk
              {...form.getInputProps('password')}
            />
            <Button type="submit" loading={login.isPending} fullWidth>
              Giriş yap
            </Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  )
}
