import { Alert, Button, Group, Paper, Select, SimpleGrid, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import type { UseMutationResult } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'

import { ApiRequestError, errorMessage } from '../../api/client'
import { PROJECT_LIMITS, useCreateProject, useProject, useUpdateProject, type ProjectRequest } from '../../api/projects'
import { PROJECT_STATUSES, type Project, type ProjectStatus } from '../../api/types'
import { useUsers } from '../../api/users'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import { parseId } from '../../lib/format'
import { enumOptions, PROJECT_STATUS_LABELS } from '../../lib/labels'
import { NotFoundPage } from '../NotFoundPage'

/** /projects/new */
export function NewProjectPage() {
  const mutation = useCreateProject()
  return (
    <Stack gap="md">
      <PageHeader title="Yeni proje" back={{ to: '/projects', label: 'Projeler' }} />
      <ProjectForm mutation={mutation} cancelTo="/projects" />
    </Stack>
  )
}

/** /projects/:id/edit. Form, proje yuklendikten sonra mevcut degerlerle acilir. */
export function EditProjectPage() {
  const id = parseId(useParams().id)
  return id === null ? <NotFoundPage /> : <EditProject id={id} />
}

function EditProject({ id }: { id: number }) {
  const project = useProject(id)
  const mutation = useUpdateProject(id)
  return (
    <QueryState query={project}>
      {(data) => (
        <Stack gap="md">
          <PageHeader title={`${data.name} · düzenle`} back={{ to: `/projects/${id}`, label: data.name }} />
          <ProjectForm project={data} mutation={mutation} cancelTo={`/projects/${id}`} />
        </Stack>
      )}
    </QueryState>
  )
}

/** Form alanlari metin tutar; Select bos secimde null verir. Istege donusum {@link toRequest}'te. */
interface FormValues {
  name: string
  customer: string
  description: string
  status: ProjectStatus | null
  ownerId: string | null
}

const maxLength = (limit: number, label: string) => (value: string) =>
  value.trim().length > limit ? `${label} en fazla ${limit} karakter olabilir.` : null

/** Bos birakilan istege bagli alanlar bos metin yerine null gonderilir. */
function toRequest(values: FormValues): ProjectRequest {
  const optional = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    name: values.name.trim(),
    customer: optional(values.customer),
    description: optional(values.description),
    status: values.status!,
    ownerId: Number(values.ownerId),
  }
}

interface ProjectFormProps {
  /** Verilirse duzenleme, verilmezse yeni proje. */
  project?: Project
  mutation: UseMutationResult<Project, Error, ProjectRequest>
  cancelTo: string
}

/**
 * Proje formu. Backend'in kurallari once burada denetlenir (bos ad istege gitmez); backend yine de
 * VALIDATION_ERROR donerse fieldErrors ayni adli alanlarin altina yazilir. Diger hatalar (or. sorumlu
 * bulunamadi) formun ustunde gosterilir.
 */
function ProjectForm({ project, mutation, cancelTo }: ProjectFormProps) {
  const navigate = useNavigate()
  const owners = useUsers('PROJECT_MANAGER')

  const form = useForm<FormValues>({
    initialValues: {
      name: project?.name ?? '',
      customer: project?.customer ?? '',
      description: project?.description ?? '',
      status: project?.status ?? 'PLANLANDI',
      ownerId: project?.ownerId != null ? String(project.ownerId) : null,
    },
    validate: {
      name: (value) => (value.trim() === '' ? 'Proje adı zorunludur.' : maxLength(PROJECT_LIMITS.name, 'Proje adı')(value)),
      customer: maxLength(PROJECT_LIMITS.customer, 'Müşteri'),
      description: maxLength(PROJECT_LIMITS.description, 'Açıklama'),
      status: (value) => (value === null ? 'Proje durumu zorunludur.' : null),
      ownerId: (value) => (value === null ? 'Sorumlu proje yöneticisi zorunludur.' : null),
    },
  })

  // Sorumlu listesi proje yoneticileridir; mevcut sorumlu baska roldeyse secili gorunmesi icin eklenir.
  const ownerOptions = (owners.data ?? []).map((user) => ({ value: String(user.id), label: user.fullName }))
  if (project?.ownerId != null && !ownerOptions.some((option) => option.value === String(project.ownerId))) {
    ownerOptions.push({ value: String(project.ownerId), label: project.ownerFullName ?? `#${project.ownerId}` })
  }

  const submit = form.onSubmit((values) =>
    mutation.mutate(toRequest(values), {
      onSuccess: (saved) => {
        notifications.show({ color: 'teal', message: project ? 'Proje güncellendi.' : 'Proje oluşturuldu.' })
        navigate(`/projects/${saved.id}`)
      },
      onError: (error) => {
        if (error instanceof ApiRequestError) form.setErrors(error.fieldErrorMap())
      },
    }),
  )

  // Alan hatasi tasimayan hatalar (404 sorumlu yok, ag hatasi...) formun ustunde gosterilir.
  const generalError =
    mutation.isError && !(mutation.error instanceof ApiRequestError && mutation.error.fieldErrors.length > 0)

  return (
    <Paper withBorder p="md" maw={720}>
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          {generalError && (
            <Alert color="red" title="Proje kaydedilemedi">
              {errorMessage(mutation.error)}
            </Alert>
          )}
          <TextInput label="Proje adı" withAsterisk {...form.getInputProps('name')} />
          <TextInput label="Müşteri" {...form.getInputProps('customer')} />
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            <Select
              label="Durum"
              withAsterisk
              data={enumOptions(PROJECT_STATUSES, PROJECT_STATUS_LABELS)}
              allowDeselect={false}
              {...form.getInputProps('status')}
            />
            <Select
              label="Sorumlu proje yöneticisi"
              withAsterisk
              placeholder={owners.isPending ? 'Yükleniyor…' : 'Seçin'}
              data={ownerOptions}
              {...form.getInputProps('ownerId')}
            />
          </SimpleGrid>
          <Textarea label="Açıklama" autosize minRows={3} maxRows={8} {...form.getInputProps('description')} />
          <Group justify="flex-end">
            <Button component={Link} to={cancelTo} variant="default">
              Vazgeç
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {project ? 'Kaydet' : 'Oluştur'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Paper>
  )
}
