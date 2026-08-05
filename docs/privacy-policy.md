# Privacy Policy — Memento Launcher

**Effective date:** 6 August 2026
**App:** Memento Launcher (`com.betteruniverse.mementolauncher`)
**Publisher:** Better Universe
**Contact:** dmvr1205@gmail.com

## The short version

Memento Launcher has **no servers, no accounts, no analytics, no advertising, and no crash
reporting**. The app does not declare the Android `INTERNET` permission, which means the
operating system itself prevents it from opening any network connection. Nothing you do in
Memento is ever transmitted to us or to any third party. We could not collect your data even
if we wanted to.

Everything the app knows lives in its private storage on your device, under your control.
The rest of this document explains precisely what that is.

## Information stored on your device

All of the following is kept in the app's private storage on your device only. We never
receive any of it.

**Life calendar**

- Your **birth date**, if you choose to provide one. It is used solely to compute the number
  of weeks you have lived, which the app displays as a dot grid.
- Your chosen **life expectancy** (a number of years).

**Launcher personalisation**

- The apps you pin as **favourites** and place in the two **dock corners** (stored as app
  package names).
- **Custom names** you give to apps.
- **Folders** you create (their names and which apps they contain).
- Apps you choose to **hide** from the app drawer, and apps you mark as **distracting**
  (which show a mindful pause before opening).
- Your **mindful pause message**, if you write a custom one.
- Appearance and behaviour settings (theme, clock style, font size, and similar).

## Information the app reads but does not keep

- **Screen time (optional).** If — and only if — you grant the *Usage access* special
  permission in Android system settings, the app reads your device's app-usage events to
  compute a single number: today's total screen-on time. This number is held in memory only,
  is never written to storage, and per-app details are never kept. Revoke the permission in
  system settings at any time and the feature stops.
- **Your list of installed apps.** Being a home screen, Memento must know which apps can be
  launched (their names and package identifiers). This is inherent to any launcher. The live
  list is not stored; only the subsets you explicitly personalise (favourites, folders,
  hidden, distracting, renames) are saved, as described above.
- **Your next alarm time**, shown on the home screen. Read from the system, held in memory
  only.

## How data can leave your device

The app itself transmits nothing. There are exactly three ways data it holds can leave your
device, and every one of them is controlled by you, not us:

1. **Android's own backup.** Like most Android apps, Memento participates in the operating
   system's backup service, which copies app settings — including your birth date, life
   expectancy, hidden/distracting app lists, and mindful message — to **your own Google
   account**, so they can be restored if you switch phones. This backup is managed by
   Android and Google, goes only to your account, and is never accessible to us. You can
   turn device backup off in Android settings. (Your favourites, custom app names, and
   folders are deliberately excluded from this backup.)
2. **Backup files you export.** The settings panel lets you export all app data as a JSON
   file to a location you choose. That file is created in plain text and contains your birth
   date and the app lists described above — treat it accordingly. The app keeps no copy;
   what happens to the file is up to you.
3. **The wallpaper.** If you apply the generated life-calendar wallpaper, the image visually
   encodes how many weeks you have lived (from which your approximate age could be
   inferred by anyone who sees your screen). The wallpaper becomes part of your device's
   system settings and remains until you change it — including after uninstalling the app.

When you use Memento's *Uninstall* or *App info* actions on another app, the identity of that
one app is passed to the Android system dialog that handles the request — the same thing that
happens when you uninstall an app from any launcher.

## Permissions, explained

| Permission | Why the app has it |
|---|---|
| `SET_WALLPAPER` | To apply the life-calendar image you generate as your wallpaper. |
| `PACKAGE_USAGE_STATS` (*Usage access*, optional, granted manually by you) | To compute today's total screen time, shown on the home screen. Works only after you enable it in system settings; the app functions fully without it. |
| `REQUEST_DELETE_PACKAGES` | So the *Uninstall* menu item can hand an app to the system's uninstall dialog. The system always asks you to confirm. |
| `EXPAND_STATUS_BAR` | So swiping down on the home screen opens your notification shade, as launchers do. The app cannot read your notifications. |

The app deliberately does **not** request: `INTERNET`, location, camera, microphone,
contacts, storage, or notification access. A connectivity-state permission
(`ACCESS_NETWORK_STATE`) is added automatically by a standard Android scheduling library the
app uses; it allows checking whether the network is up, not transferring data — and with no
`INTERNET` permission, no transfer is possible regardless.

## Retention and deletion

- Favourites, dock apps, folders, custom names, hidden and distracting lists: **removable
  individually** inside the app; removal deletes the stored value immediately.
- Birth date and life expectancy: **changeable** inside the app at any time. To erase them
  entirely, clear the app's data (Android Settings → Apps → Memento Launcher → Storage →
  Clear data) or uninstall the app.
- Uninstalling or clearing data removes everything in the app's storage. Two things survive
  it, both under your control: the applied **wallpaper** (change it in system settings) and
  Android's **cloud backup** of your settings (managed through your Google account and
  restorable if you reinstall; disable or delete it via your Google account's backup
  settings).

## Children

Memento Launcher is a general-audience utility and is not directed at children under 13. It
collects no data from anyone, including children.

## Changes to this policy

If a future version of the app changes what data is handled — for example, if a feature ever
required network access — this policy will be updated first, the effective date above will
change, and the store listing will reflect it. The version of this policy that applies is
always the one published at this address.

## Contact

Questions about this policy or your data: **dmvr1205@gmail.com**
