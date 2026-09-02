# SpendingTapper

A spending tracker for a Galaxy Z Flip 7 that gets out of the way. Tap the back of the
phone, punch in the amount, mark it need or want, hit save. Everything else is optional.

Nothing leaves the phone. There is no account, no sync and no network call anywhere in
the app.

---

## The idea

Launching SpendingTapper *is* the entry screen. There is no home screen to get past and
no menu to open — the keypad is there the instant the app appears, the date and time are
already filled in with "now", and saving closes the app so the next tap starts clean.

The screen shows one number you actually care about while you type: **what will be left
in this month's budget after this purchase**. It counts down as you key in the amount and
turns red if the entry would put you over.

## Opening it with a back-tap

One UI has no back-tap gesture of its own, so this comes from Samsung's Good Lock:

1. Install **Good Lock** from the Galaxy Store.
2. Inside Good Lock, install the **RegiStar** module.
3. RegiStar → **Back-tap gesture** → turn it on → **Double tap** → choose **SpendingTapper**.

Triple tap works too if you would rather keep double tap for something else. None of this
is the app's doing — it is an OS gesture pointed at an app, so if the back-tap does
nothing, the setting to check is RegiStar's, not anything in here.

## Installing it

Every push to `main` builds the APK in GitHub Actions.

1. Open the **Actions** tab → the latest **Build Android APK** run → download the
   **spendingtapper-debug-apk** artifact.
2. Unzip it and open the `.apk` on the phone.
3. Android will ask permission to install from this source — allow it for the browser or
   file manager you opened it with.

It is a debug-signed build, which installs fine but cannot be updated over a release
build (or over a debug build signed on a different machine). Uninstall first if Android
refuses the install.

## Backups

Settings → **Export CSV** hands the file to the system share sheet, which is why the app
needs no storage permission: the file goes to whatever app you pick rather than into
shared storage. It opens in any spreadsheet:

```
id,occurred_at,amount,kind,description,with_who
1,2026-03-15 09:30:00,12.34,NEED,coffee,
2,2026-03-15 19:02:00,48.00,WANT,"dinner, drinks","Sam, Alex"
```

**Import CSV** adds those rows back. Imported rows always get fresh ids, so importing a
backup into a database that already has entries merges rather than overwrites. Rows it
cannot parse are skipped and counted rather than failing the whole import.

## Settings worth knowing

- **Monthly budget** — the one number everything counts down from.
- **Cycle starts** — the day the month rolls over. Set it to 25 if you are paid on the
  25th; short months clamp sensibly (a 31 start day lands on the 28th in February).
- **Symbol** — whatever currency marker you want in front of the numbers. Display only;
  nothing is ever converted.

## Running it yourself

```bash
npm install
npm run dev     # the whole app in a browser, no phone needed
npm test        # 50 unit and DOM tests
npm run build   # type-check, then bundle into dist/
```

Building the APK locally additionally needs a JDK 21 and an Android SDK:

```bash
npm run build
npx cap sync android
cd android && ./gradlew assembleDebug
```

`npm run icons` regenerates the launcher artwork from the SVG in
`scripts/gen-icons.mjs`; `npx @capacitor/assets generate --android` fans it out into the
mipmap densities Android wants.

## How it is put together

React, zustand and Capacitor — the same stack as Payday and LetHimCook, so there is one
set of habits across all three rather than three.

```
src/
  lib/types.ts     the shapes everything else agrees on
  lib/money.ts     integer cents, keypad digits, parsing and formatting
  lib/cycle.ts     budget cycle maths — the half-open window a month means here
  lib/csv.ts       RFC 4180 export and import
  lib/store.ts     zustand, persisted to localStorage, versioned for migrations
  lib/files.ts     share-sheet save and file-picker read, with browser fallbacks
  lib/platform.ts  the one place that knows whether this is the APK or a browser
  components/      the keypad
  routes/          quick add, history, settings
```

Two decisions worth stating outright:

**Money is integer cents everywhere.** No float touches an amount at any point. A budget
that drifts by a fraction of a cent per entry is a budget you stop trusting.

**Saving closes the app.** That is deliberate, not a crash — the next back-tap should
land on an empty keypad, not on the entry you just finished. In a browser there is
nothing to close, so the form resets in place instead.

The tests cover the parts where the logic actually lives: budget cycle boundaries
(including short months, the year rollover, and a full year swept day by day to prove
consecutive cycles abut with no gaps), CSV round-tripping (commas, quotes and newlines
inside a description), the keypad-digits-to-cents path, and the entry screen end to end.

## History

This started as a Kotlin/Compose app. That version is tagged
[`kotlin-final`](../../tree/kotlin-final) if it is ever wanted back. It was rewritten to
match the stack the other two apps use — and, more usefully, so the app can be run and
tested in a browser rather than only compiled.

## License

MIT — see [LICENSE](LICENSE). Provided as-is, with no warranty.
