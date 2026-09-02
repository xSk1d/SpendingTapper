import { Capacitor } from '@capacitor/core'

/**
 * Writing a file out. On the phone this goes through the system share sheet, which
 * is why the app needs no storage permission at all: the file is handed to whatever
 * app you pick rather than written into shared storage. In a browser it is a plain
 * download.
 */
export async function saveTextFile(filename: string, text: string): Promise<void> {
  if (Capacitor.isNativePlatform()) {
    const [{ Directory, Encoding, Filesystem }, { Share }] = await Promise.all([
      import('@capacitor/filesystem'),
      import('@capacitor/share'),
    ])
    // Cache, not Documents: this is a hand-off, and leaving copies behind in the
    // user's document folder is not the app's business.
    await Filesystem.writeFile({
      path: filename,
      data: text,
      directory: Directory.Cache,
      encoding: Encoding.UTF8,
    })
    const { uri } = await Filesystem.getUri({ path: filename, directory: Directory.Cache })
    await Share.share({ title: filename, url: uri })
    return
  }

  const blob = new Blob([text], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** Reads a file the user picked. The picker itself is a plain <input type="file">,
 *  which the WebView maps onto the system document picker. */
export function readTextFile(file: File): Promise<string> {
  return file.text()
}
