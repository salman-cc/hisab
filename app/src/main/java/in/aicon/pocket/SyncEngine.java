package in.aicon.pocket;

import java.io.IOException;
import java.util.*;

/** Pure synchronization coordinator used by LedgerSession and its account stores.
 * Storage adapters enforce the transactional contract; live Google/Drive behavior still
 * requires real-device testing. No timestamp-based last-writer-wins or silent conflicts.
 */
public final class SyncEngine {
 public static final class ChangedDuringSync extends IOException {public ChangedDuringSync(){super("Ledger changed during sync; retry with a fresh snapshot");}}
 public interface Guard {void requireCurrent();}
 public interface Validate {void ledger(Map<String,SyncModel.Record> records)throws Exception;}
 public static final class RemoteOperation {
  public final String fileId,operationId;public final SyncModel.Record record;
  public RemoteOperation(String fileId,String operationId,SyncModel.Record record){
   if(fileId==null||!fileId.matches("[A-Za-z0-9_-]{1,200}"))throw new IllegalArgumentException("Invalid remote file ID");
   Identifiers.uuid(operationId);this.fileId=fileId;this.operationId=operationId;this.record=Objects.requireNonNull(record);
  }
 }
 public static final class Pending {
  public final String operationId,fileId;public final SyncModel.Record record;
  public Pending(String operationId,String fileId,SyncModel.Record record){Identifiers.uuid(operationId);this.operationId=operationId;this.fileId=fileId;this.record=Objects.requireNonNull(record);}
 }
 public static final class Snapshot {
  public final long revision;public final Map<String,SyncModel.Record> records;
  public Snapshot(long revision,Map<String,SyncModel.Record> records){this.revision=revision;this.records=Collections.unmodifiableMap(new TreeMap<>(records));}
 }
 public interface Store {
  Snapshot snapshot()throws Exception;
  /** Atomic CAS: apply all merged records or none. Changed merged records are queued
   * with stable operation UUIDs in the SAME transaction, before network upload.
   * Never replace user changes committed since expectedRevision.
   */
  boolean commitMerge(long expectedRevision,Map<String,SyncModel.Record> merged)throws Exception;
  /** Retain only against the exact snapshot reviewed by this attempt. */
  default void retainConflict(long revision,SyncModel.Conflict conflict)throws Exception{}
  default void retainConflict(long revision,SyncModel.Conflict conflict,Map<String,List<SyncModel.Record>> candidates)throws Exception{retainConflict(revision,conflict);}
  default void retainRepayment(long revision,Map<String,SyncModel.Record> merged,LedgerIntegrity.RepaymentConflict issue)throws Exception{}
  List<Pending> pending()throws Exception;
  /** Persist the pre-generated Drive file ID before network create. Returns the actual
   * reserved operation, or null if removed. Never changes an existing reservation.
   */
  Pending reserve(String operationId,String proposedFileId)throws Exception;
  /** Remove only this immutable outbox operation, not a newer edit to the same record. */
  void acknowledge(String operationId,String fileId)throws Exception;
 }
 public interface Remote {
  List<RemoteOperation> pull()throws Exception;
  String reserveFileId()throws Exception;
  void append(Pending operation)throws Exception;
  /** Only inspected content operations named by this pull can be removed. */
  void deleteContentFile(String fileId)throws Exception;
  /** Reset-tagged adapters may remove late old-epoch operations after a successful sync. */
  default int cleanupRetired()throws Exception{return 0;}
 }
 public static final class Result {
  public final int downloaded,uploaded,contentFilesRemoved;
  Result(int down,int up,int removed){downloaded=down;uploaded=up;contentFilesRemoved=removed;}
 }
 private final Store store;private final Remote remote;private final Guard guard;private final Validate validator;
 public SyncEngine(Store store,Remote remote,Guard guard,Validate validator){this.store=store;this.remote=remote;this.guard=guard;this.validator=validator;}
 public Result sync()throws Exception{
  guard.requireCurrent();List<RemoteOperation> operations=remote.pull();guard.requireCurrent();
  if(operations.size()>100000)throw new IOException("Too many sync operations for this version");
  // Order makes identical input deterministic. It does not decide which financial edit wins.
  operations=new ArrayList<>(operations);operations.sort(Comparator.comparing(o->o.fileId));
  Map<String,SyncModel.Record> cloud=new TreeMap<>();Map<String,List<SyncModel.Record>> grouped=new TreeMap<>();Map<String,String> seenFiles=new HashMap<>();
  for(RemoteOperation op:operations){
   String previous=seenFiles.put(op.fileId,op.operationId);if(previous!=null)throw new IOException("Duplicate Drive file in pull");
   grouped.computeIfAbsent(op.record.id,k->new ArrayList<>()).add(op.record);
  }
  Snapshot before=store.snapshot();
  for(SyncModel.Record local:before.records.values())grouped.computeIfAbsent(local.id,k->new ArrayList<>()).add(local);
  try{for(String id:grouped.keySet())cloud.put(id,SyncModel.mergeRecords(grouped.get(id)));}catch(SyncModel.Conflict conflict){guard.requireCurrent();store.retainConflict(before.revision,conflict,grouped);throw conflict;}
  Map<String,SyncModel.Record> merged=cloud;
  try{validator.ledger(Collections.unmodifiableMap(merged));}catch(LedgerIntegrity.RepaymentConflict issue){guard.requireCurrent();store.retainRepayment(before.revision,merged,issue);throw issue;}guard.requireCurrent();
  if(!store.commitMerge(before.revision,merged))throw new ChangedDuringSync();
  int sent=0,removed=0;
  // Every tombstone must be uploaded and acknowledged before content is scrubbed.
  // If any append fails, the sweep is skipped, retaining the evidence needed to retry.
  for(Pending pending:store.pending()){
   guard.requireCurrent();Pending reserved=pending;
   if(reserved.fileId==null){String fileId=remote.reserveFileId();guard.requireCurrent();reserved=store.reserve(pending.operationId,fileId);}
   if(reserved==null)continue;
   guard.requireCurrent();remote.append(reserved);guard.requireCurrent();store.acknowledge(reserved.operationId,reserved.fileId);sent++;
  }
  // Local writes may have arrived during upload. Do not claim an empty outbox or prune
  // against an obsolete snapshot. A following sync will safely finish that work.
  if(!store.pending().isEmpty())throw new ChangedDuringSync();
  Snapshot finalState=store.snapshot();
  for(RemoteOperation op:operations){
   if(op.record.deleted)continue;
   SyncModel.Record now=finalState.records.get(op.record.id);
   if(now==null||!now.deleted)continue;
   int causal=SyncModel.compare(op.record.clock,now.clock);
   if(causal!= -1&&causal!=0)continue;
   guard.requireCurrent();remote.deleteContentFile(op.fileId);removed++;
  }
  guard.requireCurrent();removed+=remote.cleanupRetired();guard.requireCurrent();return new Result(operations.size(),sent,removed);
 }
}
