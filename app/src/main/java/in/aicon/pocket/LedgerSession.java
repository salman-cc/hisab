package in.aicon.pocket;
import android.content.Context;import android.database.sqlite.SQLiteDatabase;import java.util.*;

/** One explicit ledger owner per session. The launcher-facing QuickActivity retains
 * this session through LedgerController; form edits and account operations use tickets.
 * Callers never receive a database from an old account picker. Every cloud edit and its
 * validated outbox capture succeed or roll back together. Session operations are
 * serialized; changing the active account closes this session.
 */
public final class LedgerSession implements AutoCloseable {
 public interface Edit {void run(SQLiteDatabase db,CatalogStore catalog,SettingsStore settings)throws Exception;}
 private final SQLiteDatabase db;private final CatalogStore catalog;private final SettingsStore settings;private final LedgerSyncStore sync;private final ResetStore resets;private final ResetIntentStore resetIntents;
 private final String subject;private boolean closed;private long generation=1,contentVersion;private int editDepth;
 public static final class Ticket {private final LedgerSession owner;private final long generation;private Ticket(LedgerSession owner,long generation){this.owner=owner;this.generation=generation;}}
 LedgerSession(SQLiteDatabase db,String subject)throws Exception{this.db=db;this.subject=subject;db.beginTransaction();try{LedgerSyncStore.schema(db);catalog=new CatalogStore(db);settings=new SettingsStore(db);sync=subject==null?null:new LedgerSyncStore(db,subject);resets=subject==null?null:new ResetStore(db,subject);resetIntents=subject==null?null:new ResetIntentStore(db,subject);db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public static LedgerSession local(Context context)throws Exception{SQLiteDatabase db=context.openOrCreateDatabase("pocket.db",0,null);try{return new LedgerSession(db,null);}catch(Exception e){db.close();throw e;}}
 /** Explicit remembered-account selection can reopen its private cache offline.
  * It does not authorize a network request, adopt local records or refresh a token. */
 public static LedgerSession remembered(Context context,AccountDirectory directory,String subject)throws Exception{if(directory.find(subject)==null)throw new IllegalArgumentException("Account must first be verified on this device");SQLiteDatabase db=context.openOrCreateDatabase(AccountScope.databaseFor(subject),0,null);try{return new LedgerSession(db,subject);}catch(Exception e){db.close();throw e;}}
 /** The factory requires a live verified account, not an email, persisted claim or parsed ID token. */
 public static LedgerSession connected(Context context,VerifiedDriveAccess access)throws Exception{Objects.requireNonNull(access).accessToken(access.subject);SQLiteDatabase db=context.openOrCreateDatabase(AccountScope.databaseFor(access.subject),0,null);try{return new LedgerSession(db,access.subject);}catch(Exception e){db.close();throw e;}}
 // Remembered account caches are opened by explicit selection; this factory only checks
 // the device's previously verified directory entry. It never authorizes or copies data.
 public static final class FormTicket {private final Ticket account;private final long version,revision;private FormTicket(Ticket account,long version,long revision){this.account=account;this.version=version;this.revision=revision;}}
 private long revisionForForm(){try{return sync==null?0:sync.snapshot().revision;}catch(Exception e){throw new IllegalStateException("Ledger state cannot be read",e);}}
 public synchronized FormTicket formTicket(){return new FormTicket(ticket(),contentVersion,revisionForForm());}
 public synchronized void requireFormCurrent(FormTicket form){if(form==null)throw new IllegalStateException("Reopen this form");requireCurrent(form.account);if(form.version!=contentVersion||form.revision!=revisionForForm())throw new IllegalStateException("Ledger changed. Reopen this form to use the latest names and balances.");}
 public synchronized void editForm(FormTicket form,Edit operation)throws Exception{requireFormCurrent(form);edit(form.account,operation);}
 synchronized SQLiteDatabase uiDatabase(){requireOpen();return db;}
 public synchronized boolean resetPending(){requireOpen();return resetIntents!=null&&resetIntents.current()!=null;}
 public synchronized boolean accountLedger(){requireOpen();return subject!=null;}
 public synchronized Ticket ticket(){requireOpen();return new Ticket(this,generation);}
 private void requireNoReset(){if(resetIntents!=null&&resetIntents.current()!=null)throw new IllegalStateException("A cloud reset is pending. Resume it before editing or syncing.");}
 private void requireOpen(){if(closed)throw new IllegalStateException("Ledger session closed");}
 public synchronized void requireCurrent(Ticket ticket){requireOpen();if(ticket==null||ticket.owner!=this||ticket.generation!=generation)throw new IllegalStateException("Ledger changed; reopen this action");}
 public synchronized void edit(Ticket ticket,Edit operation)throws Exception{requireCurrent(ticket);requireNoReset();boolean success=false;db.beginTransaction();editDepth++;try{operation.run(db,catalog,settings);requireCurrent(ticket);if(sync!=null&&editDepth==1)sync.captureLocalEdits();db.setTransactionSuccessful();success=true;}finally{editDepth--;db.endTransaction();if(success&&editDepth==0)contentVersion=Math.addExact(contentVersion,1);}}
 public synchronized SyncEngine.Snapshot snapshot(Ticket ticket)throws Exception{requireCurrent(ticket);if(sync==null)throw new IllegalStateException("Local-only ledger has no cloud state");return sync.snapshot();}
 /** Only this session's matching verified account can access the remote. Never falls back
  * to another selected Google account or the local-only database. Network runs off main. */
 private synchronized void requireSyncCurrent(Ticket ticket,ResetBarrier.View view){requireCurrent(ticket);requireNoReset();if(!resets.view().known.equals(view.known))throw new IllegalStateException("Reset history changed. Reopen this ledger.");}
 public SyncEngine.Result synchronize(Ticket ticket,VerifiedDriveAccess access)throws Exception{return synchronize(ticket,access,()->{});}
 public SyncEngine.Result synchronize(Ticket ticket,VerifiedDriveAccess access,SyncEngine.Guard selection)throws Exception{final ResetBarrier.View resetView;synchronized(this){requireCurrent(ticket);requireNoReset();if(sync==null||!subject.equals(access.subject))throw new IllegalStateException("Google account differs from this ledger");access.accessToken(subject);resetView=resets.view();}SyncEngine.Store guarded=new SyncEngine.Store(){public SyncEngine.Snapshot snapshot()throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);return sync.snapshot();}}public boolean commitMerge(long revision,Map<String,SyncModel.Record> records)throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);boolean committed=sync.commitMerge(revision,records);if(committed)contentVersion=Math.addExact(contentVersion,1);return committed;}}public void retainConflict(long revision,SyncModel.Conflict conflict,Map<String,List<SyncModel.Record>> candidates)throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);sync.retainConflict(revision,conflict,candidates);}}public void retainRepayment(long revision,Map<String,SyncModel.Record> merged,LedgerIntegrity.RepaymentConflict issue)throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);sync.retainRepayment(revision,merged,issue);}}public List<SyncEngine.Pending> pending()throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);return sync.pending();}}public SyncEngine.Pending reserve(String operation,String file)throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);return sync.reserve(operation,file);}}public void acknowledge(String operation,String file)throws Exception{synchronized(LedgerSession.this){requireSyncCurrent(ticket,resetView);sync.acknowledge(operation,file);}}};return new SyncEngine(guarded,new BarrierRemote(new DriveBarrierTransport(access,()->{selection.requireCurrent();requireSyncCurrent(ticket,resetView);}),resetView),()->{selection.requireCurrent();requireSyncCurrent(ticket,resetView);},records->LedgerWire.validate(records)).sync();}
 /** Explicit copy action only, never called as a side effect of signing in. The
  * caller shows source/local and the verified destination email before invoking it.
  * A fresh cloud pull must be empty; any existing local/cloud history blocks copy.
  */
 public int adoptLocal(Ticket destinationTicket,LedgerSession source,Ticket sourceTicket,VerifiedDriveAccess access)throws Exception{
  Objects.requireNonNull(source);if(source==this)throw new IllegalArgumentException("Source and destination differ");final ResetBarrier.View resetView;
  synchronized(this){requireCurrent(destinationTicket);requireNoReset();if(sync==null||!subject.equals(access.subject))throw new IllegalStateException("Choose the verified destination account");access.accessToken(subject);resetView=resets.view();if(!sync.snapshot().records.isEmpty())throw new IllegalStateException("Destination has sync history; no copy performed");}
  final Map<String,SyncModel.Record> records;synchronized(source){source.requireCurrent(sourceTicket);if(source.subject!=null)throw new IllegalArgumentException("Only the separate local-only ledger can be copied");records=LedgerTransfer.localSnapshot(source.db);}
  BarrierRemote remote=new BarrierRemote(new DriveBarrierTransport(access),resetView);if(!remote.pull().isEmpty())throw new IllegalStateException("Destination has cloud records; no copy performed");
  synchronized(this){requireCurrent(destinationTicket);requireNoReset();if(!resets.view().known.equals(resetView.known))throw new IllegalStateException("Reset view changed; review copy again");db.beginTransaction();try{LedgerTransfer.intoEmptyAccount(db,sync,records);db.setTransactionSuccessful();contentVersion=Math.addExact(contentVersion,1);}finally{db.endTransaction();}}
  return records.size();
 }
 public synchronized ConflictReviewStore.Review conflict(Ticket ticket)throws Exception{requireCurrent(ticket);if(sync==null)return null;return sync.conflict();}
 public synchronized void resolveConflict(Ticket ticket,String reviewKey,long revision,int selected,boolean delete)throws Exception{requireCurrent(ticket);requireNoReset();if(sync==null)throw new IllegalStateException("Local ledger has no cloud conflicts");sync.resolveConflict(reviewKey,revision,selected,delete);contentVersion=Math.addExact(contentVersion,1);}
 synchronized RepaymentReviewStore.Review repaymentReview(Ticket ticket)throws Exception{requireCurrent(ticket);return sync==null?null:sync.repaymentReview();}
 synchronized void removeReviewedRepayment(Ticket ticket,String key,long revision,String payment)throws Exception{requireCurrent(ticket);requireNoReset();if(sync==null)throw new IllegalStateException("Local ledger has no sync review");sync.removeReviewedRepayment(key,revision,payment);contentVersion=Math.addExact(contentVersion,1);}
 public synchronized void beginCloudReset(Ticket ticket){requireCurrent(ticket);if(resetIntents==null)throw new IllegalStateException("Local ledger is not in Google Drive");requireNoReset();resetIntents.begin(UUID.randomUUID().toString());generation=Math.addExact(generation,1);contentVersion=Math.addExact(contentVersion,1);}
 /** Run on a worker. Remains blocked on failures; signing out never claims deletion. */
 public int resumeCloudReset(Ticket ticket,VerifiedDriveAccess access)throws Exception{final ResetCoordinator coordinator;synchronized(this){requireCurrent(ticket);if(sync==null||!subject.equals(access.subject))throw new IllegalStateException("Reconnect the reset's Google account");access.accessToken(subject);coordinator=new ResetCoordinator(db,subject,new DriveBarrierTransport(access),()->requireCurrent(ticket),this);}int removed=coordinator.resume();synchronized(this){requireCurrent(ticket);generation=Math.addExact(generation,1);contentVersion=Math.addExact(contentVersion,1);}return removed;}
 /** The UI must show the reset and obtain an explicit discard choice first. */
 public synchronized void acknowledgeReset(Ticket ticket,ResetBarrier.Check observed)throws Exception{requireCurrent(ticket);if(resets==null)throw new IllegalStateException("Local ledger has no cloud reset history");db.beginTransaction();try{resets.applyObserved(observed);db.setTransactionSuccessful();}finally{db.endTransaction();}generation=Math.addExact(generation,1);contentVersion=Math.addExact(contentVersion,1);}
 public synchronized void invalidateForms(){requireOpen();generation=Math.addExact(generation,1);}
 @Override public synchronized void close(){if(closed)return;generation=Math.addExact(generation,1);closed=true;db.close();}
}
