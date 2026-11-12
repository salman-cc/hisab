# Hisab pre-redesign audit

**Audit date:** 12 November 2026 (UTC)
**Branch:** `arena/78d36a09-hisab`
**Baseline:** `37eca27d4d734996e2641e2f6ed50910eb860360`
**Scope:** source, Android configuration, local ledger/account/session/sync/reset flows, backup/export boundaries, privacy disclosures, tests, and build/CI setup. This was a source review, not a release certification.

## Executive assessment

Hisab remains a **private development preview, not production-ready**. The source contains meaningful data-separation, stale-operation, validation, and recovery safeguards, but code inspection cannot establish device behavior or end-to-end correctness. The audit environment has no Java/JDK, Gradle, Android SDK, or emulator. The newly added Gradle wrapper and CI workflow therefore have not run; neither have current Android tests or APK/manifest checks. Keep release, publication, and reliance on Google sync blocked pending the gates below.

The audit made limited, reviewable changes: centralized CSV formula-prefix escaping, added backup-size boundary enforcement, made QA artifact paths portable, added a checked-in Gradle wrapper and pinned CI workflow, corrected stale integration comments, and reconciled documentation with the current source inventory. These changes are also unverified by Android execution.

## Source-level observations

### Local ledger and account separation

- The local-only ledger uses `pocket.db`; account ledgers use a subject-derived database name. Source tracing through `QuickActivity`, `LedgerController`, and `LedgerSession` shows account selection/session ownership rather than automatic local-ledger adoption. Sign-in verification by itself does not select, copy, or upload local records.
- Session/form tickets and serialized edits are used to reject stale UI or asynchronous work. `LedgerSyncStore` composes visible ledger tables with canonical sync state/outbox updates; source transactions validate the combined ledger before accepting a merge. This is a code-path observation, not proof under real concurrency or device/process failure.
- `Ledger.amount()` converts decimal values to exact integer minor units and bounds entry size. The reviewed ledger integrity paths validate record identities, types, amounts, dates, account/catalog references, linked debts, and aggregate repayments. No independent migration, fuzz, or whole-app correctness campaign was run in this pass.

### Google and Drive

- `GoogleConnection` obtains an access token and `VerifiedDriveAccess` checks the subject and verified email against Google's HTTPS user-info response before constructing an account session. Tokens are held in memory in the reviewed path; live identity, expiry, cancellation, revocation, and process-death behavior remain unverified.
- `DriveAppData`/`DriveBarrierTransport` use Drive app-data files and stable operation IDs; `SyncEngine`, account stores, and reset coordination are connected in source. Immutable operation files avoid relying on mutable-file compare-and-swap behavior. Real Google API behavior, offline multi-device races, deletion cleanup, reset recovery, quota/rate-limit behavior, and WorkManager delivery still require device/account testing.
- The requested `userinfo.profile` scope is not required by the current authorization check and the user-info parser reads subject/email fields, not profile name/photo. The privacy disclosure now says so. No OAuth scope was silently changed; the owner should decide whether to retain or remove it and then verify consent/publication settings.
- Google OAuth testing/publication status was not independently inspected for this audit. Existing project notes describe a testing state; confirm the live Google Cloud configuration before relying on that status.

### Privacy, permissions, and recovery boundaries

- Source configuration identifies package `in.aicon.pocket.test`, versionCode 4, versionName `0.2.2-dev`, compile SDK 36, target SDK 35, and min SDK 26. The source manifest disables Android app-data backup and cleartext traffic. It declares Internet, Contacts, notifications, and boot/time-change handling. No SMS, accessibility, or notification-listener permission/service was found in the reviewed source. These are source-manifest observations only; merged APK permissions were not checked here.
- Automatic SMS sending is absent in the reviewed source; SMS/WhatsApp paths hand off reviewed text to another app, where the user must send. Local reminders are opt-in and generic. No Play Protect classification or policy outcome can be inferred from those source facts.
- Encrypted backups use PBKDF2-HMAC-SHA256 and AES-GCM in the reviewed code. Profile name/photo, contact links, and pay-by notes are documented as device-private and outside ledger backup/sync. CSV/PDF are plaintext, readable exports; Drive sync is not an encrypted backup or application-level end-to-end encryption.
- Import already rejects files above 8 MiB. This audit added the same maximum to generated encrypted exports and an exact-boundary unit test. Neither the test nor backup flow was run; existing files above the import limit remain unimportable in the current app.

## Changes made on this branch

1. **CSV export:** formula-prefix and leading-control handling now lives in `Ledger.csv()` for both export call sites. It covers `=`, `+`, `-`, and `@` after leading whitespace/BOM and protects leading tab/CR/LF, while retaining CSV quoting and quote escaping. Added `LedgerCsvTest` for the cases. Execution is pending.
2. **Backup limit:** added `MainActivity.backupFitsRestoreLimit()` and refuse to save an encrypted backup larger than the existing 8 MiB import ceiling; added `BackupFlowTest.encryptedBackupExportLimitMatchesRestoreLimit`. Execution is pending.
3. **QA artifacts:** removed `/downloads/hisab-v2/qa` from UI test helpers. `QaArtifacts` defaults to `${java.io.tmpdir}/hisab-v2/qa`, permits an explicit `-Dhisab.qa.dir=...`, creates the directory, and rejects path-containing artifact names.
4. **Reproducible build entry point:** added Gradle 8.14.3 wrapper scripts/JAR and updated README commands. The official Gradle 8.14.3 wrapper JAR SHA-256 is `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`, matching Gradle wrapper-validation data. This verifies the wrapper JAR, **not** the Gradle distribution archive. An authoritative distribution checksum was not established, so `distributionSha256Sum` remains unset.
5. **CI:** added `.github/workflows/android-ci.yml`, pinning checkout, Java setup, Android setup, and Gradle wrapper validation actions to reviewed commit IDs; it provisions Java 17/SDK 36 and runs debug assembly plus `testDebugUnitTest`. The workflow has not run.
6. **Documentation/comments:** corrected comments that described connected account/session/sync/reset code as unwired; recorded historical versus current test evidence; clarified the profile-scope behavior and OAuth-status uncertainty in documentation.

## Verification performed and limits

- After the final edits, `git diff --check` passed. A parser-only syntax check passed for 77 Java source/test files (`javalang`; not compilation or type checking); `sh -n gradlew` and a YAML syntax parse of the CI workflow also passed.
- Source inventory currently reports 131 `@Test` methods across 19 test classes. The historical `VERIFICATION.md` record lists 125 earlier methods; four later `LocalUiTest` methods and two audit-added regressions bring the source count to 131. Counts are not passing-test evidence.
- No Android compile, unit-test task, APK inspection, device/emulator test, OAuth/Drive round trip, or root Java program ran in this audit. The historical test/APK claims in `VERIFICATION.md` are explicitly retained as inherited reports, not reproduced evidence.
- No dependency vulnerability review, external penetration test, backup migration campaign, accessibility review, performance test, or Google Cloud configuration review was performed.

## Release gates

1. On a provisioned host, run `./gradlew --no-daemon --stacktrace :app:assembleDebug :app:testDebugUnitTest`; obtain a green result for the added workflow and review test reports, APK permissions, package/version, and signing identity. Treat existing historical results separately.
2. Establish the Gradle 8.14.3 distribution SHA-256 through an authoritative Gradle source before setting `distributionSha256Sum`; keep wrapper validation enabled.
3. Exercise backup creation/import at, below, and above 8 MiB, including a valid historical backup, interrupted file-picker operations, corrupt ciphertext, wrong passphrases, and replacement after intervening edits.
4. Perform controlled real-device tests for account mismatch, sign-in/reconnect, scope denial, token expiry, disconnect/revoke, Drive sync, offline edits, multi-device conflicts/deletions, reset markers, cleanup, retries, and background scheduling. Use disposable test accounts and ledgers.
5. Confirm Google OAuth scopes, tester/publishing state, privacy disclosures, current target/API requirements, package/signing plan, support/recovery process, and device/OS coverage with the owner. Decide the Hisab source-code licence before any public distribution.

**Disposition:** source review completed; no release approval. Current test/build state is **unverified**, and the app must remain a development preview until the release gates are closed.
