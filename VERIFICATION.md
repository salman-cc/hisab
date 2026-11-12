# 0.2.2-dev verification record

## Earlier reported run — historical, not reproduced here

The existing verification record reports 125 distinct passing framework methods across 18 classes, including 39 LocalUiTest methods in seven bounded groups. This inherited report is not evidence about the current source tree: four additional LocalUiTest methods were added later, and the current source has another regression test added during this audit. The table and method list below preserve the earlier report rather than claim a fresh run. Simulated Android checks are not a real-device pass.

| Class | Methods | Failures |
|---|---:|---:|
| AccountDirectoryTest | 3 | 0 |
| AccountUiTest | 9 | 0 |
| BackgroundSyncTest | 7 | 0 |
| BackupFlowTest | 3 | 0 |
| BarrierRemoteTest | 5 | 0 |
| DuePlansTest | 3 | 0 |
| LedgerControllerTest | 7 | 0 |
| LedgerDeviceSyncTest | 2 | 0 |
| LedgerSessionTest | 6 | 0 |
| LedgerSyncStoreTest | 13 | 0 |
| LedgerTransferTest | 3 | 0 |
| LocalUiTest | 39 | 0 |
| ReminderScheduleTest | 4 | 0 |
| RepaymentReviewTest | 4 | 0 |
| ResetCoordinatorTest | 3 | 0 |
| ResetStoreTest | 3 | 0 |
| SyncStoreTest | 8 | 0 |
| SyncWorkSchedulerTest | 3 | 0 |

## Methods

### in.aicon.pocket.AccountDirectoryTest
- forgetReturnsToLocalWithoutClaimingCloudDeletion
- unknownAndCorruptSelectionCannotOpenLedger
- verificationDoesNotSelectOrAdoptAndSelectionSurvivesReopen

### in.aicon.pocket.AccountUiTest
- conflictDescriptionShowsMoneyAsCurrencyNotMinorUnits
- disconnectConfirmationCancelKeepsAccountAndPermissionRequestUnstarted
- documentPickerReturnFromClosedLedgerIsDiscarded
- localAndRememberedAccountScreensAndSeparateLedgerSwitch
- oldResetConfirmationCannotResetDifferentSelectedLedger
- postedBackupActionAndReplacementTicketCannotCrossLedgerSwitchOrEdit
- repaymentReviewRendersOriginalDebtAndBothUnchosenPayments
- replacementDialogCannotRestoreAfterNewLocalChanges
- resetDialogCancelCannotClearMoneyAndNamesExpectedAccount

### in.aicon.pocket.BackgroundSyncTest
- differentGoogleIdentityNeverRunsAndRequiresReconnect
- pendingDisconnectStopsEvenAlreadyAuthorizedBackgroundWork
- pendingResetStopsBeforeAuthorizationAndEdit
- queuedOldAccountCannotAuthorizeWhenSelectionChanged
- rateLimitsRetryButPermissionRequestsStayQuiet
- selectedAccountSyncClearsItsTransientTokenAndSavesOnlyStatus
- selectionChangeDuringAuthorizationStopsBeforeAnySync

### in.aicon.pocket.BackupFlowTest
- decryptReachesReplacementConfirmationWithoutMutatingLedgerVersion
- decryptedReplacementCannotOverwriteInterveningNewEntry
- encryptedBackupPassphraseFormDoesNotWriteOrInvalidateLedger

### in.aicon.pocket.BarrierRemoteTest
- appendInFlightAtResetNeverReturnsAfterBarrierAcknowledgment
- futureBarrierOperationForcesRecheckInsteadOfSilentDrop
- missingResetHistoryAndUnknownDeletionStopBeforeEffects
- retiredLateUploadIsScrubbedOnNextSuccessfulSyncCycle
- wireRejectsHiddenPayloadInMarkerAndDuplicateBarrierIds

### in.aicon.pocket.DuePlansTest
- changedDateClearsAutomaticGrant
- dateTriggersOnlyAfterPromisedDayAndNoRepeat
- legacyAutomaticSmsGrantIsScrubbedWithoutSending

### in.aicon.pocket.LedgerControllerTest
- disconnectRevokesExactAccountThenReturnsLocalWithoutErasingCachedMoney
- failedDisconnectPersistsAndBlocksReauthorizationUntilExplicitRetry
- forgetDoesNotDeleteCloudOrMoveCachedLedger
- restoringAccountCacheRequiresReconnectAndNeverReusesAnotherToken
- staleDisconnectTargetCannotRevokeAnotherSelectedAccount
- staleNetworkCompletionCannotRepaintClosedController
- verifiedIdentityNeverSelectsCopiesOrSyncs

### in.aicon.pocket.LedgerDeviceSyncTest
- localCurrencyRelabelAttemptRollsBackPreferencesAndOutbox
- twoDevicesKeepOfflineRepaymentLinkedAcrossAccountAndPersonRenames

### in.aicon.pocket.LedgerSessionTest
- formCannotOverwriteBackgroundMergeThroughAnotherDatabaseHandle
- invalidatedFormsAndOtherSessionTicketsCannotWrite
- localSessionMigratesOriginalSchemaWithoutUploading
- pendingResetBlocksEditsAndOldFormsBeforeAnyUpload
- sessionEditCapturesPreferencesCatalogAndMoneyTogether
- validationFailureRollsBackMoneyCatalogPreferenceAndOutbox

### in.aicon.pocket.LedgerSyncStoreTest
- captureRollbackRestoresOriginalLedger
- catalogRenameResolvesExistingReferenceAndPreferenceRoundTrips
- deleteGoalAndRecreateGetsNewIdentity
- emptyCatalogPlaceholdersNeverReuseDeletedCloudIdentities
- firstEmptyCloudSyncKeepsUncommittedStarterCatalogAndEmptyHistory
- invalidCombinedRepaymentsKeepAllTablesAndOutbox
- mergedQuickDefaultCannotSilentlySelectMissingOrWrongAccount
- multipleConflictChoicesApplyWholeCandidateLedgerOnlyAfterFinalReview
- sameSeedLabelsHaveSameIdsAcrossFreshDevices
- staleReviewDoesNotOverwriteNewLocalEdit
- threeWayConflictRetainsEveryChoiceAndResolutionDominatesAll
- transactionsBudgetsGoalsCaptureAndReloadAtomically
- wrongEntityReferenceAndMixedCurrencyRejectAtomically

### in.aicon.pocket.LedgerTransferTest
- explicitCopyKeepsSourceAndLinksAndCapturesNewAccountClock
- failedCopyTransactionKeepsEmptyTargetAndSource
- repeatCopyAndNonlocalSourceAreRejectedWithoutReplacing

### in.aicon.pocket.LocalUiTest
- budgetAndGoalEditingPreserveIdentity
- contactMenuOnlyOffersReviewedMessages
- customizationBackupRoundtripIncludesNewChoices
- customizationEditorRendersAndSaves
- customizationRejectsDuplicatesAndFailedRestoreIsAtomic
- customizationRenamePreservesDebtLinksTransfersBudgetsAndBackup
- darkMalayalamRenders
- debtEntryPartialRepaymentOverpaymentAndCascadeDelete
- debtPeopleRender
- deleteAllLocalClearsPersonalCatalogAndResetsCustomDefaults
- deviceFeedbackBackHomeColoursAndBranding
- disappearedDeleteTargetNeverDeletesOtherRecords
- floatingDockAnimationHasIntermediateFrames
- glassHomeBottomAndLargeFontsRemainReachable
- invalidPreferenceAndFractionalBackupLeaveDataUnchanged
- largeIconChooserRenders
- largerFontEntryRemainsScrollable
- mixedPersonLedgerAndPhotoAreDevicePrivate
- monthNavigationDayGroupsAndSwipeNeverDeletes
- monthlyChartsPhotosAndFloatingGlassRender
- oldBackupWithMultipleBudgetsAndGoalsMigrates
- openEntryCannotRecreateRenamedAccountFromStaleChoices
- originalCategoryMarksAndLauncherVariants
- overviewAndEntryRender
- pdfPaginationAndMalayalamStayWithinPage
- polishedSettingsLightAndDarkMalayalamRender
- preferencesFormattingDefaultsRenameAndBackup
- profileDetailsDueMessagesPdfAndFontsRender
- remindersContactsAndPersonLedgerHaveExplicitLocalBoundaries
- removeUnusedEntityResetsDefaultsAndKeepsReferencedHistory
- resetConfirmationCancelAndConfirmUseCurrentLedger
- revisedThemeChoicesRenderAndEnforceGlassDarkOnly
- selectedMonthReportChartsAndFutureExclusionRender
- settledDebtClearsPrivateDuePlan
- sidewaysSwipeRevealsActionsAndDeleteStillAsks
- staleComposerReviewRejectsChangedContactAndLedger
- staleUiFormCannotWriteAndFailedSaveRollsBack
- systemThemeFollowsDeviceAndGlassStaysDark
- twoThousandEntryLocalMutationPassesDirectAndBackupValidation

### in.aicon.pocket.ReminderScheduleTest
- defaultsOffAndTwoLocalTimes
- deniedPermissionDoesNotScheduleAndBootReschedulesOnlyEnabled
- notificationHasNoPrivateAmountsOrPeople
- timezoneAndNextDayRollover

### in.aicon.pocket.RepaymentReviewTest
- invalidCombinedRepaymentsStageWithoutChangingMoneyThenExplicitDeletionCommits
- severalOverpaidDebtsKeepEveryChoiceStagedUntilWholeLedgerIsValid
- staleOrNonPaymentChoicesCannotRemovePrincipal
- syncEngineRetainsInvalidMergedCandidateBeforeAnyUploadOrMoneyReplacement

### in.aicon.pocket.ResetCoordinatorTest
- failedCleanupRetainsIntentAndDoesNotRepublishOrResurrect
- lostPublishResponseRetriesSameMarkerAndReservation
- noIntentOrAccountGuardPreventsAnyRemoteMutation

### in.aicon.pocket.ResetStoreTest
- explicitAcknowledgmentClearsAllFinancialAndOutboxBytesAtomically
- rollbackKeepsFinancialDataPendingUploadsAndPriorBarrierView
- untransactionalOrStaleAcknowledgmentCannotClearLedger

### in.aicon.pocket.SyncStoreTest
- accountMismatchAndUntransactionalEditAreRejected
- concurrentLocalEditRejectsOldMerge
- localEditAndDeleteAreAtomicAndContentFree
- remoteMergeRetainsTombstonesAndRejectsRewind
- reservationSurvivesReopenAndAcknowledgesOnlyExactOperation
- rollbackDoesNotLeaveOutboxOrRecord
- shortenedIdsAndInvalidCatalogNumbersAreRejected
- strictJsonRejectsContentInDeletionAndFractionalClock

### in.aicon.pocket.SyncWorkSchedulerTest
- reopeningSameLedgerDoesNotCancelItsPendingEditJob
- repeatedEditEnqueueKeepsOnePendingJobAndLocalSelectionCancelsIt
- requestsUseSelectedSubjectNetworkConstraintsAndInexactThirtyMinuteCadence

The earlier report states that packaged APK permissions, version and signing certificate were independently checked and that legacy automatic-SMS cleanup, reviewed composers and stale contact/ledger guards passed. Those APK and execution results were not reproduced during this audit. Real Google sign-in/Drive round trips, phone installation and Play Protect outcome remain unverified. The eight root Java programs are unchanged; their earlier run evidence is not relabelled as a new run.

## Current source inventory — 12 November 2026

A source scan finds 131 `@Test` methods across 19 test classes: the 125 methods listed above, four later LocalUiTest methods, the `LedgerCsvTest.formulaPrefixesAndLeadingControlsAreEscapedAsText` CSV regression, and `BackupFlowTest.encryptedBackupExportLimitMatchesRestoreLimit`. The four later UI methods are `budgetProgressUsesSaturatedExactIntegerPercentages`, `chartTouchAnnouncesExactCumulativeAndCategoryAmounts`, `entrySearchAndKindFiltersStayWithinTheSelectedMonth`, and `monthlyCategoryChartLocalizesBuiltInLabels`. Source annotation counts are inventory only, not executed-test evidence.

GitHub Actions run [43143726314](https://github.com/salman-cc/hisab/actions/runs/43143726314) on commit `572628b571ff0ed1d1ed7633f51520c48fa7a29f` completed successfully on 12 November 2026: wrapper validation, Java 17 setup, Android SDK 36 setup, `:app:assembleDebug`, and `:app:testDebugUnitTest` all passed. The source inventory remains 131 test methods; the workflow summary did not provide an extracted per-method count. This is CI evidence, not a device or Play verification.

The local workspace has no Java/Javac, Gradle, Android SDK or emulator, so no local build or test ran. The initial CI run warned that the pinned action versions were using deprecated Node 20 and that `ubuntu-latest` was migrating. The workflow has since been updated to Node 24 action releases and Ubuntu 24.04; that refreshed workflow has not yet run. No merged-APK permission/signing inspection, device test, or root Java program run is claimed.
