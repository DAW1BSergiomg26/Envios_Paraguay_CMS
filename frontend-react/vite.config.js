import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  base: process.env.VITE_BASE || '/',
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      strategies: 'injectManifest',
      srcDir: 'public',
      filename: 'sw.js',
      includeAssets: ['favicon.svg', 'icons/*.svg'],
      manifest: {
        name: 'Monteastur Envios',
        short_name: 'Monteastur',
        description: 'Plataforma logística España ↔ Paraguay',
        theme_color: '#0f1117',
        background_color: '#0f1117',
        display: 'standalone',
        display_override: ['standalone', 'minimal-ui'],
        start_url: process.env.VITE_START_URL || '/',
        scope: process.env.VITE_BASE || '/',
        orientation: 'portrait-primary',
        lang: 'es',
        categories: ['logistics', 'business', 'shipping'],
        icons: [
          {
            src: 'icons/icon-192.svg',
            sizes: '192x192',
            type: 'image/svg+xml'
          },
          {
            src: 'icons/icon-512.svg',
            sizes: '512x512',
            type: 'image/svg+xml'
          },
          {
            src: 'icons/icon-512.svg',
            sizes: '512x512',
            type: 'image/svg+xml',
            purpose: 'maskable'
          }
        ],
        screenshots: [
          {
            src: 'screenshots/dashboard.png',
            sizes: '1280x720',
            type: 'image/png',
            form_factor: 'wide',
            label: 'Panel de administración Monteastur'
          }
        ]
      },
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        maximumFileSizeToCacheInBytes: 4 * 1024 * 1024
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        navigateFallback: (process.env.VITE_BASE || '/') + 'index.html',
        navigateFallbackDenylist: [/^\/api/, /^\/actuator/, /^\/login/, /^\/logout/],
        runtimeCaching: [
          {
            urlPattern: /^\/api\/.*/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 30,
                maxAgeSeconds: 60
              },
              networkTimeoutSeconds: 5
            }
          }
        ]
      }
    })
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8895',
        changeOrigin: true,
        secure: false
      },
      '/actuator': {
        target: 'http://localhost:8895',
        changeOrigin: true,
        secure: false
      },
      '/login': {
        target: 'http://localhost:8895',
        changeOrigin: true,
        secure: false
      },
      '/logout': {
        target: 'http://localhost:8895',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'http://localhost:8895',
        changeOrigin: true,
        secure: false
      },
      '/ws': {
        target: 'http://localhost:8895',
        ws: true,
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rolldownOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/recharts')
              || id.includes('node_modules/victory-vendor')
              || id.includes('node_modules/d3-')) return 'vendor-charts';
          if (id.includes('node_modules/xlsx')) return 'vendor-xlsx';
          return undefined;
        }
      }
    }
  }
})
