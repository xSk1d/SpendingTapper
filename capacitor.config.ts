import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  // Permanent once the app is published, so it matches the package the Kotlin
  // build already claimed rather than inventing a second identity.
  appId: 'dev.xsk1d.spendingtapper',
  appName: 'SpendingTapper',
  // The Vite build is copied into the APK, so the app carries its own assets and
  // never needs a network or a host.
  webDir: 'dist',
  android: {
    backgroundColor: '#12100E',
  },
}

export default config
