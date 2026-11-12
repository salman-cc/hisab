package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.util.*;import org.json.*;

/** Account-ledger adapter used by LedgerSession for canonical records and sync state.
 * Capture shares the user's edit transaction. Remote merge atomically updates canonical
 * records, outbox and visible tables, or keeps all previous data on error. Live Google
 * and Drive behavior still requires real-device verification.
 */
public final class LedgerSyncStore implements SyncEngine.Store {
 private final SQLiteDatabase db;private final SqliteSyncStore state;private final CatalogStore catalog;private final SettingsStore settings;private final ConflictReviewStore reviews;private final RepaymentReviewStore repayments;
 public LedgerSyncStore(SQLiteDatabase db,String subject)throws Exception{this.db=db;state=new SqliteSyncStore(db,subject);schema(db);catalog=new CatalogStore(db);settings=new SettingsStore(db);reviews=new ConflictReviewStore(db,subject);repayments=new RepaymentReviewStore(db,subject);}
 public static void schema(SQLiteDatabase db){
  db.execSQL("CREATE TABLE IF NOT EXISTS tx(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT NOT NULL,amount INTEGER NOT NULL,day TEXT NOT NULL,account TEXT NOT NULL,dest TEXT NOT NULL,category TEXT NOT NULL,note TEXT NOT NULL,uuid TEXT NOT NULL,person TEXT NOT NULL DEFAULT '',loan TEXT NOT NULL DEFAULT '')");
  Set<String> txColumns=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(tx)",null)){while(c.moveToNext())txColumns.add(c.getString(1));}for(String column:new String[]{"uuid","person","loan"})if(!txColumns.contains(column))db.execSQL("ALTER TABLE tx ADD COLUMN "+column+" TEXT NOT NULL DEFAULT ''");try(Cursor c=db.rawQuery("SELECT id FROM tx WHERE uuid=''",null)){while(c.moveToNext()){ContentValues v=new ContentValues();v.put("uuid",UUID.randomUUID().toString());db.update("tx",v,"id=?",new String[]{c.getString(0)});}}
  db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tx_uuid ON tx(uuid)");
  db.execSQL("CREATE TABLE IF NOT EXISTS budgets(category TEXT PRIMARY KEY,amount INTEGER NOT NULL,uuid TEXT NOT NULL DEFAULT '')");
  db.execSQL("CREATE TABLE IF NOT EXISTS goals(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,target INTEGER NOT NULL,saved INTEGER NOT NULL,uuid TEXT NOT NULL DEFAULT '')");
  for(String table:new String[]{"budgets","goals"}){
   Set<String> cols=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())cols.add(c.getString(1));}if(!cols.contains("uuid"))db.execSQL("ALTER TABLE "+table+" ADD COLUMN uuid TEXT NOT NULL DEFAULT ''");
  }
  try(Cursor c=db.rawQuery("SELECT category FROM budgets WHERE uuid=''",null)){while(c.moveToNext()){ContentValues v=new ContentValues();v.put("uuid",UUID.randomUUID().toString());db.update("budgets",v,"category=?",new String[]{c.getString(0)});}}
  db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS budgets_uuid ON budgets(uuid)");
  try(Cursor c=db.rawQuery("SELECT id FROM goals WHERE uuid=''",null)){while(c.moveToNext()){ContentValues v=new ContentValues();v.put("uuid",UUID.randomUUID().toString());db.update("goals",v,"id=?",new String[]{c.getString(0)});}}
  db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS goals_uuid ON goals(uuid)");
 }
 private Map<String,String> visible()throws Exception{return LedgerRecords.capture(db,catalog,settings);}
 boolean owns(SQLiteDatabase database){return db==database;}
 public void captureLocalEdits()throws Exception{
  if(!db.inTransaction())throw new IllegalStateException("Capture must be in the ledger edit transaction");
  Map<String,String> visible=visible();SyncEngine.Snapshot old=state.snapshot();for(SyncModel.Record previous:old.records.values())if(!previous.deleted){JSONObject j=new JSONObject(previous.payload);if(Arrays.asList("transaction","budget","goal").contains(j.optString("type"))&&!j.optString("currency","INR").equals(settings.get("currency")))throw new IllegalArgumentException("Clear the ledger before changing currency; no implicit conversion");}Map<String,SyncModel.Record> check=new TreeMap<>();
  for(String id:visible.keySet())check.put(id,new SyncModel.Record(id,visible.get(id),false,Collections.emptyMap()));LedgerWire.validate(check);
  for(String id:visible.keySet()){
   SyncModel.Record previous=old.records.get(id);String payload=visible.get(id);
   if(previous==null||previous.deleted||!payload.equals(previous.payload))state.localEdit(id,payload,false);
  }
  for(SyncModel.Record previous:old.records.values())if(!previous.deleted&&!visible.containsKey(previous.id))state.localEdit(previous.id,null,true);
 }
 public void retainConflict(long revision,SyncModel.Conflict conflict)throws Exception{Map<String,List<SyncModel.Record>> candidates=new TreeMap<>();for(SyncModel.Record record:state.snapshot().records.values())candidates.put(record.id,Collections.singletonList(record));candidates.put(conflict.local.id,conflict.versions);retainConflict(revision,conflict,candidates);}
 @Override public void retainConflict(long revision,SyncModel.Conflict conflict,Map<String,List<SyncModel.Record>> candidates)throws Exception{db.beginTransaction();try{if(state.snapshot().revision!=revision)throw new SyncEngine.ChangedDuringSync();reviews.save(revision,conflict,candidates);repayments.clear();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public ConflictReviewStore.Review conflict()throws Exception{return reviews.current();}
 /** Apply no partial ledger. Choices remain private staged review context until all
  * conflicting records are chosen and the entire candidate ledger is valid. */
 public boolean resolveConflict(String reviewKey,long reviewedRevision,int selected,boolean delete)throws Exception{db.beginTransaction();try{ConflictReviewStore.Review review=reviews.current();SyncEngine.Snapshot current=state.snapshot();if(review==null||!review.key.equals(reviewKey)||review.revision!=reviewedRevision||current.revision!=reviewedRevision)throw new IllegalStateException("Ledger changed. Sync again and review the current versions.");if(!delete&&(selected<0||selected>=review.conflict.versions.size()))throw new IllegalArgumentException("Choose a reviewed version");String payload=delete?null:review.conflict.versions.get(selected).payload;SyncModel.Record chosen=SyncModel.resolve(review.conflict,state.deviceId(),payload,delete);Map<String,List<SyncModel.Record>> candidates=new TreeMap<>(review.candidates);candidates.put(chosen.id,Collections.singletonList(chosen));Map<String,SyncModel.Record> merged=new TreeMap<>();try{for(String id:candidates.keySet())merged.put(id,SyncModel.mergeRecords(candidates.get(id)));}catch(SyncModel.Conflict next){reviews.save(current.revision,next,candidates);db.setTransactionSuccessful();return false;}try{LedgerWire.validate(merged);}catch(LedgerIntegrity.RepaymentConflict issue){repayments.save(current.revision,merged);reviews.clear();db.setTransactionSuccessful();return false;}if(!commitMerge(current.revision,merged))throw new SyncEngine.ChangedDuringSync();reviews.clear();repayments.clear();db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
 @Override public void retainRepayment(long revision,Map<String,SyncModel.Record> merged,LedgerIntegrity.RepaymentConflict issue)throws Exception{db.beginTransaction();try{if(state.snapshot().revision!=revision)throw new SyncEngine.ChangedDuringSync();repayments.save(revision,merged);reviews.clear();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 RepaymentReviewStore.Review repaymentReview()throws Exception{return repayments.current();}
 boolean removeReviewedRepayment(String key,long revision,String paymentId)throws Exception{db.beginTransaction();try{RepaymentReviewStore.Review review=repayments.current();SyncEngine.Snapshot current=state.snapshot();if(review==null||!review.key.equals(key)||review.revision!=revision||current.revision!=revision)throw new IllegalStateException("Ledger or review changed. Sync and review again.");if(!review.issue.repayments.contains(paymentId))throw new IllegalArgumentException("Choose a repayment from this review");Map<String,SyncModel.Record> records=new TreeMap<>(review.records);SyncModel.Record payment=records.get(paymentId);if(payment==null||payment.deleted)throw new IllegalArgumentException("Repayment no longer available");records.put(paymentId,payment.edit(state.deviceId(),null,true));try{LedgerWire.validate(records);}catch(LedgerIntegrity.RepaymentConflict next){repayments.save(revision,records);db.setTransactionSuccessful();return false;}if(!commitMerge(revision,records))throw new SyncEngine.ChangedDuringSync();repayments.clear();db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
 @Override public SyncEngine.Snapshot snapshot()throws Exception{return state.snapshot();}
 @Override public boolean commitMerge(long revision,Map<String,SyncModel.Record> merged)throws Exception{
  Map<String,JSONObject> decoded=LedgerWire.validate(merged);
  db.beginTransaction();try{
   if(!state.commitMerge(revision,merged))return false;
   // A brand-new empty Drive ledger must not erase the UI's uncommitted starter
   // catalog or write sync history that would block an explicit local copy.
   if(merged.isEmpty()){try(Cursor count=db.rawQuery("SELECT (SELECT count(*) FROM tx)+(SELECT count(*) FROM budgets)+(SELECT count(*) FROM goals)",null)){count.moveToFirst();if(count.getLong(0)!=0)throw new IllegalArgumentException("Empty merge cannot erase unsynced money");}reviews.clear();repayments.clear();db.setTransactionSuccessful();return true;}
   // Preserve local row IDs for existing UUIDs so open navigation references stay stable.
   Map<String,Long> txIds=ids("tx"),goalIds=ids("goals");JSONArray catalogRows=new JSONArray();JSONObject preferences=null;for(JSONObject j:decoded.values()){if(j.getString("type").equals("catalog"))catalogRows.put(new JSONObject().put("uuid",j.getString("uuid")).put("type",j.getString("entityType")).put("name",j.getString("name")).put("icon",j.getInt("icon")).put("color",j.getString("color")));else if(j.getString("type").equals("preferences"))preferences=j.getJSONObject("values");}catalog.restore(catalogRows);settings.restore(preferences==null?new JSONObject():preferences);
   db.delete("tx",null,null);db.delete("budgets",null,null);db.delete("goals",null,null);
   for(JSONObject j:decoded.values()){
    String type=j.getString("type"),uuid=j.getString("uuid");if(type.equals("catalog")||type.equals("preferences"))continue;ContentValues v=new ContentValues();
    if(type.equals("transaction")){
     v.put("uuid",uuid);if(txIds.containsKey(uuid))v.put("id",txIds.get(uuid));for(String key:new String[]{"kind","day","account","dest","category","note","person","loan"})v.put(key,j.getString(key));v.put("amount",j.getLong("amount"));db.insertOrThrow("tx",null,v);
    }else if(type.equals("budget")){v.put("uuid",uuid);v.put("category",j.getString("category"));v.put("amount",j.getLong("amount"));db.insertOrThrow("budgets",null,v);
    }else{v.put("uuid",uuid);if(goalIds.containsKey(uuid))v.put("id",goalIds.get(uuid));v.put("name",j.getString("name"));v.put("target",j.getLong("target"));v.put("saved",j.getLong("saved"));db.insertOrThrow("goals",null,v);}
   }
   reviews.clear();repayments.clear();db.setTransactionSuccessful();return true;
  }finally{db.endTransaction();}
 }
 private Map<String,Long> ids(String table){Map<String,Long> result=new HashMap<>();try(Cursor c=db.rawQuery("SELECT uuid,id FROM "+table,null)){while(c.moveToNext())result.put(c.getString(0),c.getLong(1));}return result;}
 @Override public List<SyncEngine.Pending> pending()throws Exception{return state.pending();}
 @Override public SyncEngine.Pending reserve(String operation,String fileId)throws Exception{return state.reserve(operation,fileId);}
 @Override public void acknowledge(String operation,String fileId)throws Exception{state.acknowledge(operation,fileId);}
}
