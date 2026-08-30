# SpendingTapper

A spending tracker for a Galaxy Z Flip 7 that gets out of the way. Tap the back of the
phone, punch in the amount, mark it need or want, hit save. Everything else is optional.

Nothing leaves the phone. The manifest requests **no permissions** — no internet, no
storage, no location. (`aapt dump badging` shows one entry, a signature-level permission
scoped to the app's own package that androidx adds automatically; it grants nothing and
never appears to you.)

---

## The idea

Launching SpendingTapper *is* the entry screen. There is no home screen to get past and no menu to
open — the keypad is there the instant the app appears, the date and time are already
filled in with "now", and saving closes the app so the next tap starts clean.

The screen shows one number you actually care about while you type: **what will be left
in this month's budget after this purchase**. It counts down as you key in the amount and
turns red if the entry would put you over.

## Opening it with a back-tap

One UI has no back-tap gesture of its own, so this comes from Samsung's Good Lock:

1. Install **Good Lock** from the Galaxy Store.
2. Inside Good Lock, install the **RegiStar** module.
3. RegiStar → **Back-tap gesture** → turn it on → **Double tap** → choose **SpendingTapper**.

Triple tap works too if you would rather keep double tap for something else. If Good Lock
is not available in your region, the app also publishes an "Add expense" launcher
shortcut (long-press the icon), which most third-party gesture apps can target.

### On the closed phone

To use SpendingTapper on the 4.1" cover screen, turn on **Settings → Advanced features → Labs →
apps allowed on the cover screen** (Good Lock's MultiStar module does the same thing on
older builds), then add SpendingTapper to the allowed list.

The entry screen adapts on its own: below about 620dp of height it keeps the amount, the
need/want toggle, the keypad and Save on screen, and folds the description, the "with
who" chips and the date and time away behind a **Details** button. Unfolded, everything
shows at once. Folding or unfolding mid-entry does not lose what you have typed.

The keypad is the app's own rather than the system keyboard — it is on screen instantly,
never resizes the window, and stays thumb-sized on the cover display.

## Installing it

Every push builds the APK in GitHub Actions.

1. Open the **Actions** tab → the latest **SpendingTapper** run → download the **spendingtapper-apk**
   artifact.
2. Unzip it and open `app-release.apk` on the phone.
3. Android will ask permission to install from this source — allow it for the browser or
   file manager you opened it with.

The release APK is about 3 MB.

### Installing updates over the top

By default CI signs the release with a throwaway debug key, which is different on every
run, so a new build will not install over an old one. To fix that, generate a keystore
once and give it to Actions:

```bash
keytool -genkeypair -v -keystore spendingtapper-release.jks -alias spendingtapper \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 spendingtapper-release.jks    # macOS: base64 -i spendingtapper-release.jks
```

Then add four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `SPENDINGTAPPER_KEYSTORE_BASE64` | the base64 output above |
| `SPENDINGTAPPER_KEYSTORE_PASSWORD` | the keystore password |
| `SPENDINGTAPPER_KEY_ALIAS` | `spendingtapper` |
| `SPENDINGTAPPER_KEY_PASSWORD` | the key password |

Keep `spendingtapper-release.jks` somewhere safe and out of the repo — losing it means having to
uninstall and reinstall to move to a new key. The build also reads these from
`spendingtapper/app/keystore.properties` (gitignored) if you would rather build locally:

```properties
storeFile=app/spendingtapper-release.jks
storePassword=…
keyAlias=spendingtapper
keyPassword=…
```

## Backups

Settings → **Export CSV** writes a plain file through the system file picker (which is
why no storage permission is needed). It opens in any spreadsheet:

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
- **Symbol** — whatever currency marker you want in front of the numbers.

## Building it yourself

```bash
cd spendingtapper
./gradlew test            # 32 unit tests
./gradlew lintDebug
./gradlew assembleRelease  # app/build/outputs/apk/release/app-release.apk
```

Needs JDK 21 and an Android SDK (`local.properties` with `sdk.dir=…`, or `ANDROID_HOME`
set). Gradle comes from the wrapper.

## How it is put together

Kotlin, Jetpack Compose and Room, one module, one activity.

```
app/src/main/java/dev/xsk1d/spendingtapper/
  MainActivity.kt      the only activity; the quick-add screen is the start destination
  SpendingTapperApp.kt          AppContainer — three dependencies wired by hand, no DI framework
  data/                Room entity, DAO, database, repository, DataStore settings
  domain/              BudgetCycle (cycle maths) and Money (integer cents)
  io/                  CSV export and an RFC 4180 reader
  ui/quickadd/         the entry screen, its ViewModel and the keypad
  ui/history/          grouped list with per-day and per-month totals
  ui/settings/         budget, cycle, currency, export and import
```

Two decisions worth stating outright:

**Money is integer cents everywhere.** No `Double` touches an amount at any point. A
budget that drifts by a fraction of a cent per entry is a budget you stop trusting.

**The budget figure is a `SUM` in SQL**, not a fold over loaded rows, so "left this
month" stays instant no matter how many years of entries accumulate.

The tests cover the parts where the logic actually lives: budget cycle boundaries
(including the short-month and year-rollover cases, and a full year swept day by day to
prove consecutive cycles abut with no gaps), CSV round-tripping (commas, quotes and
newlines inside a description), and the keypad-digits-to-cents path.

## License

MIT — see [LICENSE](LICENSE). Provided as-is, with no warranty.
