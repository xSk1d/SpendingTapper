import { useRef } from 'react'

type Props = {
  onDigit: (digit: string) => void
  onBackspace: () => void
  onClear: () => void
}

const BACKSPACE = '⌫'

const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0', BACKSPACE]

/**
 * The app's own keypad rather than the system keyboard. It is on screen the
 * instant the app opens, it never resizes the window mid-entry, and its keys stay
 * thumb-sized on the Flip's 4.1" cover display where a software keyboard is close
 * to unusable.
 *
 * Keys act on `click`, not on `pointerdown`. Pointer events are only used to time
 * the hold-to-clear gesture — a key that fires on pointerdown alone is dead to
 * keyboard activation and to anything driving the page programmatically.
 */
export default function Keypad({ onDigit, onBackspace, onClear }: Props) {
  const longPress = useRef<number | null>(null)
  const cleared = useRef(false)

  const cancelHold = () => {
    if (longPress.current === null) return
    clearTimeout(longPress.current)
    longPress.current = null
  }

  const startHold = (key: string) => {
    if (key !== BACKSPACE) return
    // Holding backspace wipes the amount, so a mistyped entry does not need
    // twelve taps to undo.
    cleared.current = false
    cancelHold()
    longPress.current = window.setTimeout(() => {
      cleared.current = true
      onClear()
    }, 500)
  }

  const activate = (key: string) => {
    cancelHold()
    if (key === BACKSPACE) {
      // The click that ends a hold-to-clear must not also delete a digit.
      if (cleared.current) {
        cleared.current = false
        return
      }
      onBackspace()
      return
    }
    if (key === '00') {
      onDigit('0')
      onDigit('0')
      return
    }
    onDigit(key)
  }

  return (
    <div className="keypad">
      {KEYS.map((key) => (
        <button
          key={key}
          type="button"
          className="key"
          aria-label={key === BACKSPACE ? 'Delete last digit' : key}
          onClick={() => activate(key)}
          onPointerDown={() => startHold(key)}
          onPointerUp={cancelHold}
          onPointerLeave={cancelHold}
        >
          {key}
        </button>
      ))}
    </div>
  )
}
