import { Capacitor } from '@capacitor/core'

/**
 * Saving closes the app so the next back-tap starts clean — that is the whole
 * point of the entry flow, and it is deliberate rather than a crash.
 *
 * There is no app to close in a browser, so the caller supplies what should
 * happen instead. Kept behind one function so no screen has to know whether it
 * is running inside the APK.
 */
export function closeApp(fallback?: () => void): void {
  if (Capacitor.isNativePlatform()) {
    // Imported lazily: the web build should not pull in a native-only plugin.
    void import('@capacitor/app').then(({ App }) => App.exitApp())
    return
  }
  fallback?.()
}

export function isNative(): boolean {
  return Capacitor.isNativePlatform()
}
