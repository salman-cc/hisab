package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Transactional per-account canonical sync state and outbox, composed by
 * LedgerSyncStore with the visible tx/budget/goal adapter. LedgerSession coordinates
 * both under the account session. Never attach this store to the local-only database.
 */
public final class SqliteSyncStore implements SyncEngine.Store {
 private final SQLiteDatabase db;
 public SqliteSyncStore(SQLiteDatabase db,String verifiedSubject)throws Exception {
  if(!new java.io.File(db.getPath()).getName().equals(AccountScope.databaseFor(verifiedSubject)))throw new IllegalArgumentException("Sync DB must match the verified Google subject");this.db=db;
  db.beginTransaction();try{
   db.execSQL("CREATE TABLE IF NOT EXISTS cloud_meta(name TEXT PRIMARY KEY,value TEXT NOT NULL)");
   db.execSQL("CREATE TABLE IF NOT EXISTS cloud_records(uuid TEXT PRIMARY KEY,body TEXT NOT NULL)");
   db.execSQL("CREATE TABLE IF NOT EXISTS cloud_outbox(operation TEXT PRIMARY KEY,file_id TEXT,body TEXT NOT NULL)");
   String bound=meta("subject");if(bound==null){putMeta("subject",verifiedSubject);putMeta("device",UUID.randomUUID().toString());putMeta("revision","0");}else if(!bound.equals(verifiedSubject))throw new IllegalStateException("Stored Google account differs");
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
 }
 private String meta(String name){try(Cursor c=db.rawQuery("SELECT value FROM cloud_meta WHERE name=?",new String[]{name})){return c.moveToFirst()?c.getString(0):null;}}
 private void putMeta(String name,String value){ContentValues v=new ContentValues();v.put("name",name);v.put("value",value);if(db.insertWithOnConflict("cloud_meta",null,v,SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new android.database.SQLException("Could not persist sync metadata");}
 String deviceId(){return Objects.requireNonNull(meta("device"));}
 private long revision(){return Long.parseLong(Objects.requireNonNull(meta("revision")));}
 private void advance(){putMeta("revision",Long.toString(Math.addExact(revision(),1L)));}
 private void putRecord(SyncModel.Record r)throws Exception{ContentValues v=new ContentValues();v.put("uuid",r.id);v.put("body",SyncJson.record(r));if(db.insertWithOnConflict("cloud_records",null,v,SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new android.database.SQLException("Could not persist sync record");}
 private void queue(SyncModel.Record r)throws Exception{
  // A later causal record covers unsent older local operations. In particular, do
  // not upload a locally deleted payload merely because it was once queued.
  List<String> superseded=new ArrayList<>();
  try(Cursor c=db.rawQuery("SELECT operation,body FROM cloud_outbox",null)){while(c.moveToNext()){SyncModel.Record old=SyncJson.parseRecord(c.getString(1));if(old.id.equals(r.id)&&same(SyncModel.merge(old,r),r))superseded.add(c.getString(0));}}
  for(String op:superseded)db.delete("cloud_outbox","operation=?",new String[]{op});
  ContentValues v=new ContentValues();v.put("operation",UUID.randomUUID().toString());v.put("body",SyncJson.record(r));db.insertOrThrow("cloud_outbox",null,v);}
 private Map<String,SyncModel.Record> records()throws Exception {Map<String,SyncModel.Record> result=new TreeMap<>();try(Cursor c=db.rawQuery("SELECT uuid,body FROM cloud_records",null)){while(c.moveToNext()){SyncModel.Record r=SyncJson.parseRecord(c.getString(1));if(!r.id.equals(c.getString(0)))throw new IllegalStateException("Record ID mismatch");result.put(r.id,r);}}return result;}
 @Override public SyncEngine.Snapshot snapshot()throws Exception {db.beginTransaction();try{SyncEngine.Snapshot snapshot=new SyncEngine.Snapshot(revision(),records());db.setTransactionSuccessful();return snapshot;}finally{db.endTransaction();}}
 /** Called inside the same DB transaction as the ledger edit. A deletion cannot be
  * edited back to life. Re-adding the entry must use a fresh UUID.
  */
 public void localEdit(String id,String payload,boolean deleted)throws Exception{
  if(!db.inTransaction())throw new IllegalStateException("Ledger and outbox must share a transaction");
  SyncModel.Record prior=null;try(Cursor c=db.rawQuery("SELECT body FROM cloud_records WHERE uuid=?",new String[]{id})){if(c.moveToFirst())prior=SyncJson.parseRecord(c.getString(0));}
  if(prior==null)prior=new SyncModel.Record(id,payload,deleted,new TreeMap<>());
  SyncModel.Record next=prior.edit(Objects.requireNonNull(meta("device")),payload,deleted);putRecord(next);queue(next);advance();
 }
 @Override public boolean commitMerge(long expectedRevision,Map<String,SyncModel.Record> merged)throws Exception {
  db.beginTransaction();try{
   if(revision()!=expectedRevision)return false;
   Map<String,SyncModel.Record> old=records();if(!merged.keySet().containsAll(old.keySet()))throw new IllegalArgumentException("Merge may not discard records or tombstones");
   boolean changed=false;
   for(SyncModel.Record record:merged.values()){
    SyncModel.Record previous=old.get(record.id);
    if(previous!=null&&SyncJson.record(previous).equals(SyncJson.record(record)))continue;
    // Validate causal continuity. The caller may add remote versions, not rewind state.
    if(previous!=null&&!same(SyncModel.merge(previous,record),record))throw new IllegalArgumentException("Merge would rewind the local state");
    putRecord(record);queue(record);changed=true;
   }
   if(changed)advance();db.setTransactionSuccessful();return true;
  }finally{db.endTransaction();}
 }
 private static boolean same(SyncModel.Record a,SyncModel.Record b){return a.id.equals(b.id)&&a.deleted==b.deleted&&Objects.equals(a.payload,b.payload)&&a.clock.equals(b.clock);}
 @Override public List<SyncEngine.Pending> pending()throws Exception{List<SyncEngine.Pending> result=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT operation,file_id,body FROM cloud_outbox ORDER BY rowid",null)){while(c.moveToNext())result.add(new SyncEngine.Pending(c.getString(0),c.isNull(1)?null:c.getString(1),SyncJson.parseRecord(c.getString(2))));}return result;}
 @Override public SyncEngine.Pending reserve(String operation,String fileId)throws Exception {
  if(fileId==null||!fileId.matches("[A-Za-z0-9_-]{1,200}"))throw new IllegalArgumentException("Invalid Drive file ID");
  db.beginTransaction();try{
   try(Cursor c=db.rawQuery("SELECT file_id,body FROM cloud_outbox WHERE operation=?",new String[]{operation})){
    if(!c.moveToFirst()){db.setTransactionSuccessful();return null;}
    String actual=c.isNull(0)?fileId:c.getString(0);if(c.isNull(0)){ContentValues v=new ContentValues();v.put("file_id",fileId);db.update("cloud_outbox",v,"operation=? AND file_id IS NULL",new String[]{operation});}
    SyncEngine.Pending pending=new SyncEngine.Pending(operation,actual,SyncJson.parseRecord(c.getString(1)));db.setTransactionSuccessful();return pending;
   }
  }finally{db.endTransaction();}
 }
 @Override public void acknowledge(String operation,String fileId){db.delete("cloud_outbox","operation=? AND file_id=?",new String[]{operation,fileId});}
}
