import {
  Anchor,
  AppShell,
  createTheme,
  NavLink,
  Paper,
  Table,
  type CSSVariablesResolver,
  type MantineColorsTuple,
} from '@mantine/core'

/**
 * Koyu tema skalasi: indigoya hafif egilimli soguk lacivert-gri. Mantine koyu yuzeyleri bu tonlardan alir;
 * uygulama yalnizca koyu temada calisir (main.tsx: forceColorScheme).
 */
const ink: MantineColorsTuple = [
  '#E3E7EF', // 0: govde metni
  '#C5CBD8', // 1: tablo basligi metni
  '#9DA5B8', // 2: soluk metin (dimmed)
  '#737C91', // 3: placeholder, ok ikonlari
  '#3A4254', // 4: kart, input ve tablo kenarligi
  '#2F3646', // 5: satir hover
  '#272D3B', // 6: input, acilir liste, zebra satir
  '#1E2431', // 7: govde zemini; kartlar ve modallar
  '#1A1F2B', // 8: ust bar ve sol menu
  '#12161F', // 9: icerik zemini
]

/** Uygulamanin Mantine temasi; testler de ayni temayla render eder (src/test/render.tsx). */
export const theme = createTheme({
  primaryColor: 'indigo',
  // Dolu butonlarda yazi/zemin 5.1:1.
  primaryShade: { light: 8, dark: 8 },
  // Dolu buton ve rozet yazilari saf beyaz yerine hafif kirik beyaz; saf siyah da kullanilmaz.
  white: '#F1F3F8',
  black: ink[9],
  colors: { dark: ink },
  // Koyu zeminde golge ancak koyu ve yogun olunca secilir; ayrimi asil kenarlik saglar.
  shadows: {
    xs: '0 1px 2px rgb(4 6 12 / 0.35)',
    sm: '0 2px 4px rgb(4 6 12 / 0.35), 0 6px 14px -4px rgb(4 6 12 / 0.45)',
  },
  defaultRadius: 'md',
  components: {
    // Ust bar ve menu kartlardan bir ton koyu, icerik zemini en koyu: kartlar zeminden ayrilir.
    AppShell: AppShell.extend({
      styles: {
        header: { backgroundColor: 'var(--mantine-color-dark-8)' },
        navbar: { backgroundColor: 'var(--mantine-color-dark-8)' },
        main: { backgroundColor: 'var(--mantine-color-dark-9)' },
      },
    }),
    // Varsayilan indigo.4 hover satirinda AA'nin altinda kalir; bir ton acik.
    Anchor: Anchor.extend({ defaultProps: { c: 'indigo.3' } }),
    Paper: Paper.extend({ defaultProps: { shadow: 'xs' } }),
    // Tablo basliklari geri planda kalir; goz satirlara gider.
    Table: Table.extend({
      styles: {
        thead: { backgroundColor: 'var(--mantine-color-dark-8)' },
        th: { color: 'var(--mantine-color-dark-1)', fontWeight: 600 },
      },
    }),
    NavLink: NavLink.extend({
      styles: { root: { borderRadius: 'var(--mantine-radius-md)' }, label: { fontWeight: 500 } },
    }),
  },
})

/** Uygulamada kullanilan renk aileleri (rozetler, hafif ve subtle butonlar). */
const VARIANT_COLORS = ['indigo', 'red', 'teal', 'yellow', 'orange', 'grape', 'blue', 'green', 'pink', 'violet', 'cyan', 'lime']

/**
 * Mantine koyu temada light/subtle yazilarini 0. tonla (neredeyse beyaz) yazar: "Sil" ve "Duzenle" gibi butonlar
 * rengini kaybeder. 3. ton hem kendi light zemininde hem hover satirinda >= 5:1. Gri rozet zemini de neredeyse
 * siyah yerine kenarlik tonunda. main.tsx ve test/render.tsx MantineProvider'a verir.
 */
export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: {},
  light: {},
  dark: {
    ...Object.fromEntries(VARIANT_COLORS.map((c) => [`--mantine-color-${c}-light-color`, `var(--mantine-color-${c}-3)`])),
    '--mantine-color-gray-light': 'var(--mantine-color-dark-4)',
    '--mantine-color-gray-light-hover': 'var(--mantine-color-dark-3)',
    '--mantine-color-gray-light-color': 'var(--mantine-color-dark-1)',
  },
})
