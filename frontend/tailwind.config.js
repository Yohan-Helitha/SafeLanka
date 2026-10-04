/** @type {import('tailwindcss').Config} */
const c = (name) => `rgb(var(--${name}) / <alpha-value>)`

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: c('canvas'),
        panel: c('panel'),
        raised: c('raised'),
        line: c('line'),
        ink: c('ink'),
        muted: c('muted'),
        faint: c('faint'),
        action: c('action'),
        signal: c('signal'),
        ok: c('ok'),
        caution: c('caution'),
        danger: c('danger'),
        sidebar: '#0B1324',
        topbar: '#0F172A',
        sev: {
          advisory: '#2563EB',
          watch: '#CA8A04',
          warning: '#EA580C',
          evacuate: '#DC2626',
        },
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        display: ['"IBM Plex Sans Condensed"', '"IBM Plex Sans"', 'system-ui', 'sans-serif'],
      },
      borderRadius: { card: '10px', control: '8px' },
    },
  },
  plugins: [],
}
