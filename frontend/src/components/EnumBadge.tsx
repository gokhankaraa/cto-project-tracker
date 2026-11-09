import { Badge, Text, type BadgeProps } from '@mantine/core'

import type { EnumLabels } from '../lib/labels'

interface EnumBadgeProps<E extends string> extends Omit<BadgeProps, 'color' | 'children'> {
  value: E | null | undefined
  labels: EnumLabels<E>
}

/**
 * Bir enum degerini Turkce etiketi ve rengiyle rozet olarak gosterir
 * (or. <EnumBadge value="YUKSEK" labels={RISK_LEVEL_LABELS} />). Deger yoksa (rapor yok) tire gosterir.
 */
export function EnumBadge<E extends string>({ value, labels, ...props }: EnumBadgeProps<E>) {
  if (value == null) {
    return (
      <Text span c="dimmed" size="sm">
        –
      </Text>
    )
  }
  const { label, color } = labels[value]
  return (
    <Badge color={color} variant="light" {...props}>
      {label}
    </Badge>
  )
}
