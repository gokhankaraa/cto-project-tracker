import { TextInput } from '@mantine/core'
import { useDebouncedCallback } from '@mantine/hooks'
import { useState } from 'react'

/**
 * Sorumlu aramasi: kutu yazildikca guncellenir, filtre yazmayi birakinca (300 ms) uygulanir; her tusta
 * istek gitmez. Adresteki deger disaridan degisirse (Filtreleri temizle, geri tusu) kutu ona uyar.
 */
export function AssigneeInput({ value, onChange }: { value: string | null; onChange: (value: string | null) => void }) {
  const [text, setText] = useState(value ?? '')
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    // Render sirasinda esitleme (effect yerine): React'in "onceki prop'a gore state ayarlama" kalibi.
    setLastValue(value)
    if ((value ?? '') !== text.trim()) setText(value ?? '')
  }
  const apply = useDebouncedCallback((next: string) => onChange(next.trim() === '' ? null : next.trim()), 300)

  return (
    <TextInput
      label="Sorumlu"
      placeholder="Ad ile ara"
      value={text}
      onChange={(event) => {
        setText(event.currentTarget.value)
        apply(event.currentTarget.value)
      }}
    />
  )
}
