import { Group, Progress, Text } from '@mantine/core'

/** Ilerleme yuzdesi: kisa bir cubuk ve yaninda "%75". Tablolarda kullanilir. */
export function ProgressValue({ value, label }: { value: number; label: string }) {
  return (
    <Group gap="xs" wrap="nowrap">
      <Progress value={value} w={72} aria-label={label} />
      <Text size="sm">%{value}</Text>
    </Group>
  )
}
