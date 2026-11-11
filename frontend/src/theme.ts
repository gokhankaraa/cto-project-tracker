import { Anchor, createTheme, NavLink, Paper, Table, type MantineColorsTuple } from '@mantine/core'

/**
 * Indigoya hafif egilimli soguk gri skala. Saf beyaz yerine kirik beyaz yuzeylerle birlikte
 * parlamayi azaltir; 6. ton (c="dimmed") hem kartta hem gri zeminde AA kontrastini saglar.
 */
const slate: MantineColorsTuple = [
  '#EAEDF2', // 0: tablo basligi, zebra satir
  '#E2E6ED', // 1: icerik zemini, satir hover
  '#D8DDE6',
  '#CBD1DC', // 3: kart ve tablo kenarligi
  '#B8BECC', // 4: input kenarligi
  '#959CAE', // 5: placeholder
  '#5C6376', // 6: soluk metin (dimmed)
  '#464C5E',
  '#323746',
  '#1F2330',
]

/** Uygulamanin Mantine temasi; testler de ayni temayla render eder (src/test/render.tsx). */
export const theme = createTheme({
  primaryColor: 'indigo',
  // 7. ton: kirik beyaz yuzeyde buton yazisi ve baglantilar AA kontrastta kalir.
  primaryShade: { light: 7, dark: 8 },
  // Kartlar, ust bar, menu, inputlar ve acilir pencereler saf beyaz yerine bu kirik beyazi kullanir.
  white: '#F3F5F8',
  black: slate[9],
  colors: { gray: slate },
  shadows: {
    xs: '0 1px 2px rgb(31 35 48 / 0.06), 0 1px 3px rgb(31 35 48 / 0.04)',
    sm: '0 1px 3px rgb(31 35 48 / 0.08), 0 4px 10px -2px rgb(31 35 48 / 0.08)',
  },
  defaultRadius: 'md',
  components: {
    // Zebra satirlarda da AA kontrast icin baglantilar bir ton koyu.
    Anchor: Anchor.extend({ defaultProps: { c: 'indigo.8' } }),
    // Kartlar gri zeminden kenarlik ve hafif bir golgeyle ayrilir (AppLayout icerik alani gray.1).
    Paper: Paper.extend({ defaultProps: { shadow: 'xs' } }),
    // Tablo basliklari geri planda kalir; goz satirlara gider.
    Table: Table.extend({
      styles: {
        thead: { backgroundColor: 'var(--mantine-color-gray-0)' },
        th: { color: 'var(--mantine-color-gray-7)', fontWeight: 600 },
      },
    }),
    NavLink: NavLink.extend({
      styles: { root: { borderRadius: 'var(--mantine-radius-md)' }, label: { fontWeight: 500 } },
    }),
  },
})
