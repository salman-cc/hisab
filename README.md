# Hisab 0.2.2 development candidate

Original Android personal ledger, separate from the AiCoN reader. Package `in.aicon.pocket.test`, versionCode 4. This is a private work-in-progress candidate, not a ready release. No licence for the Hisab code has been selected; source visibility is not an open-source licence. The explicitly bundled Comfortaa, Inter, Lora and Poppins fonts have their own SIL OFL 1.1 licences in app assets.

Local-only is the default. Optional Google account UI and own-Drive app-data sync are enabled in this candidate, with INTERNET permission. Actual Google sign-in and Drive end-to-end behavior have NOT been verified on a phone or emulator. Do not treat compiled code or simulated tests as a live pass.

## Build
Use Java 17, the checked-in Gradle 8.14.3 wrapper, AGP 8.13.2 and Android SDK platform 36. Set ANDROID_HOME or an untracked local.properties with sdk.dir, then run `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`). Dependencies come from Google Maven and Maven Central. No SDK, signing key, credentials, caches or private build overrides are included. CI validates the wrapper JAR and runs the debug build plus the framework-test task. The wrapper distribution URL is version-pinned; its official distribution SHA-256 still needs independent verification before adding `distributionSha256Sum`.

Run `./gradlew :app:testDebugUnitTest` for Android-framework simulation tests. On limited hosts, run bounded groups of classes rather than one all-class native-graphics run. Root Java *Test.java programs are pure checks; compile with their corresponding pure model classes. These are not phone performance or OS execution tests.

The registered Android OAuth client is tied to the private preview certificate and this package. Ordinary debug keys are different and cannot reuse that Android registration unchanged. Client IDs are public configuration, not secrets. Never include a client secret or signing key in source.

## New candidate features
- Back closes dialogs first, then returns to Overview, then exits.
- Visible colour swatches, original Hisab wordmark/header, top-right profile and device-private profile photo.
- Dynamic light/dark/pure-black appearance; separate dark-only reflective Glass with a blurred floating dock and animated selection pill.
- Month/year picker, income/expense/net summaries, day-grouped Activity, daily totals and swipe-to-reveal Edit/Delete. Deletion still asks. Net excludes transfers/debts and is not the account balance.
- Selected-month cumulative expense line and category donut. Charts exclude dates after today and show recorded values, not a forecast. Monthly headline totals include all recorded dates in that month.
- People ledger with separate receive/pay figures and chronological signed running balance, linked repayments and safe principal limits.
- Optional selected contact link, saved only on this phone. Contacts permission is requested when choosing a contact, not on launch. WhatsApp/SMS review opens another app; Hisab does not send automatically. Carrier/data charges may apply. Contact and ledger changes invalidate an old message review.
- Optional local reminders, off by default, 08:30 and 20:30 editable local times, original short tone and app notification mark. Notifications show no amounts/people/accounts. Inexact Android alarms can be delayed. Enable asks notification permission; reboot/time/timezone changes reschedule enabled reminders.
- Existing encrypted backup and CSV behavior retained. Photos and contact links stay outside ledger backups and Drive.

## Sync boundaries
Google ledgers are separate from local-only records. Sign-in alone does not copy the local ledger. An explicit copy flow checks the target. Selected-ledger edits queue a roughly 10-second coalesced job; periodic selected-ledger sync requests a 30-minute cadence when network/battery/Android permit it. Neither is instant or guaranteed delivery. Tokens are memory-only; reconnect can be needed. Account changes, reset markers, stale forms and conflicts are guarded. A real device is still required to validate Google identity, Drive scopes, disconnect/revoke, background execution and recovery.

See STATUS.md and VERIFICATION.md for the current test boundary. No ads, analytics, bank connection, automatic SMS/notification reading, AiCoN data server or paid messaging API. No calendar view in this candidate.

## Latest profile, reminders and customization changes
- Always-round top-right profile/photo control and a profile page with optional device-private display name, money-control overview, overdue list and export actions.
- System default font is first and default. Only four bundled choices: Comfortaa Medium, Inter, Lora and Poppins. Licences are readable under Appearance > Font. Missing scripts fall back to Android. On device appearance follows system light/dark; Liquid Glass stays dark-only.
- Encrypted backup/restore now carries font, theme, palette, home order, number/date/currency formatting, default entry selections, custom catalog/icon/colour choices, launcher colour and daily reminder times/on-off choice. Notification permission is not granted by restore. CSV is entry data, not a restorable settings backup; PDF is a printable raster statement, not searchable text. Profile photo/name, contact links and local pay-by notes remain device-private and are not in Drive or backups. Automatic-message grants are removed.
- Each unpaid debt can have a recorded pay-by date. It is the user's note, not proof of agreement by the other person. Optional generic local overdue alert is at 08:30 the day AFTER that date, with Android delays possible. Reviewed messages include current amount/direction/date and ask the recipient to confirm or correct the note. A settled/deleted debt clears its device-private plan when the app refreshes.
- No person-to-person Hisab live sharing or automatic acceptance/paid acknowledgement. Composer text works without a server. Own-Drive sync is only the selected user's private ledger, not another person's app inbox.

This candidate is intended for an explicitly untested prerelease. Device installation, native Google sign-in/Drive, OS background delivery, launcher refresh, Contacts/photo pickers, messaging handoff and actual SMS carrier behavior remain unverified. Keep important existing backups before installing.

## 0.2.2-dev safety update
Automatic SMS has been removed completely: no SEND_SMS permission, background SMS sender, callback receiver or automatic-send menu. Reviewed SMS/WhatsApp composers and local overdue notifications remain. Old automatic-message grant fields are stripped from device-private due plans. This minimizes permissions; it is not a verified fix for a particular Play Protect classification. No Play Protect bypass is advised. Google remains in Testing mode. Public informational pages live in docs/; publication and OAuth state are independent.
