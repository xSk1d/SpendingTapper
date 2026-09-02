import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// base: './' keeps the build portable. It works from a domain root (Vercel/Netlify)
// and from a sub-path (GitHub Pages) without a rebuild.
export default defineConfig({
  base: './',
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'apple-touch-icon-180.png'],
      manifest: {
        name: 'SpendingTapper',
        short_name: 'SpendingTapper',
        description: 'Tap, punch in the amount, save. Nothing leaves the phone.',
        theme_color: '#12100E',
        background_color: '#12100E',
        display: 'standalone',
        orientation: 'portrait',
        start_url: './',
        scope: './',
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'icon-512-maskable.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
        // The launcher shortcut the Kotlin build published, so a gesture app that
        // targets shortcuts rather than apps still has something to aim at.
        shortcuts: [
          {
            name: 'Add expense',
            short_name: 'Add',
            url: './#/',
          },
        ],
      },
    }),
  ],
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    restoreMocks: true,
  },
})
