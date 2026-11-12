# 0.2.2-dev candidate status

12 November 2026. Private development preview; not production-ready and not for release.

## Current audit pass

This branch received a source-level review and limited safe repairs. The first GitHub Actions run on commit `572628b` passed wrapper validation, debug APK assembly, and the Android unit-test task on JDK 17 / SDK 36. The local workspace still has no Java/Javac, Gradle, Android SDK or emulator, so no local build/test or device verification is claimed. The first run warned that pinned actions used deprecated Node 20 and `ubuntu-latest` was migrating; the workflow has since been refreshed to Node 24 action releases and pinned to Ubuntu 24.04, pending a rerun. No merged-APK inspection or real-device verification is claimed. See `VERIFICATION.md` and `docs/PRE_REDESIGN_AUDIT.md`.

The repository's old verification record reports 125 passing test methods. Current test source contains 131 methods across 19 test classes: four UI methods were added after the historical method list, and this audit adds CSV and backup-size regressions. The initial CI run completed the Android unit-test task successfully for the 131-method source tree, but its summary did not report an individual test count. Existing historical run claims were not independently reproduced.

## Safety and privacy boundaries

- Application identity remains `in.aicon.pocket.test`, versionCode 4, versionName `0.2.2-dev`; no signing, OAuth scope, application-data or licence decision was changed.
- The source manifest disables Android backup and cleartext traffic. It declares Internet for optional Google access, Contacts for explicitly selected contacts, notifications and boot delivery; no SMS, accessibility or notification-listener permission/service was found in source.
- Local-only records and per-Google-account ledgers remain separate. Tokens are memory-only in the reviewed code path; profile photos/name, contact links and pay-by notes are kept outside ledger sync and encrypted ledger payloads.
- CSV and PDF remain readable, unencrypted exports; only the passphrase-encrypted backup is a restorable format. Drive sync is not an independent backup and is not application-level end-to-end encrypted.
- Automatic SMS remains removed. SMS/WhatsApp handoff is user-reviewed and requires the user to send in the messaging app. Local notifications remain generic.

## Open release gates

- Keep the refreshed CI green; the first run passed on commit `572628b`, but the Node 24 / Ubuntu 24.04 action update still needs its own run. Inspect the merged APK manifest, signing and dependency results; no artifact/manifest review has been recorded.
- Verify the Gradle 8.14.3 *distribution* SHA-256 from an official Gradle source before setting `distributionSha256Sum`. The checked-in wrapper JAR SHA-256 is verified against Gradle's wrapper-validation data; that is not the distribution checksum.
- A source repair prevents new encrypted exports exceeding the existing 8 MiB restore limit; its boundary unit test was included in the successful initial CI task. The real file-picker/export/import path has not been exercised here. Previously saved files over that limit remain unimportable through the current app. Do not treat CSV/PDF as a recovery workaround.
- Review the requested `userinfo.profile` scope with the owner. Current account-verification code reads subject/email fields and does not use a Google profile name or photo; scope changes remain intentionally untouched.
- Complete real-device Google sign-in, Drive sync/reset/revoke, background scheduling, upgrade/restore, accessibility, performance and supported-OS checks. Confirm OAuth testing/publication state and release requirements with the owner.
- Decide the Hisab code licence before any public source distribution. Keep any future Play/production release blocked until signing, target/API policy, privacy disclosures and operational recovery are reviewed.

Google remains described by existing project notes as in Testing/External Testing, but that account configuration was not independently inspected. No credentials, Google Cloud changes, release, publication, deployment or merge were performed.
