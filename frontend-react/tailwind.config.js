/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        grafito: {
          50: '#e8ebf0',
          100: '#d1d9e6',
          200: '#a3b5cd',
          300: '#7591b4',
          400: '#4e7296',
          500: '#335577',
          600: '#2a455f',
          700: '#22374b',
          800: '#1a2c3d',
          900: '#111827',
          950: '#0b0f19',
        },
      },
      animation: {
        'pulse-soft': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'shimmer': 'shimmer 2s linear infinite',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
    },
  },
  plugins: [],
}