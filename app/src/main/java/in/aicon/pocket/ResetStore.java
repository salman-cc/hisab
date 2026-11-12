package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;

/** Durable local reset history for one account ledger, used by LedgerSession and
 * ResetCoordinator. This store does not contact Drive or delete remote files itself.
 * A reset marker must already be durably published and re-read before the caller can
 * acknowledge it or offer the corresponding destructive choice to the user.
 */
public final class ResetStore {
 private final SQLiteDatabase db;
 public ResetStore(SQLiteDatabase db,String subject){if(!new java.io.File(db.getPath()).getName().equals(AccountScope.databaseFor(subject)))throw new IllegalArgumentException("Reset store must match verified ledger account");this.db=db;db.execSQL("CREATE TABLE IF NOT EXISTS reset_history(uuid TEXT PRIMARY KEY)");}
 public ResetBarrier.View view(){Set<String> known=new HashSet<>();try(Cursor c=db.rawQuery("SELECT uuid FROM reset_history",null)){while(c.moveToNext())known.add(c.getString(0));}return new ResetBarrier.View(known);}
 /** Used only in the same transaction as the user's explicit clear/reset choice.
  * Financial/catalog/outbox bytes are removed before the barrier set is advanced.
  * An exception rolls back every table and the old epoch remains active.
  */
 public void applyObserved(ResetBarrier.Check observed)throws Exception{
  if(!db.inTransaction())throw new IllegalStateException("Clear ledger and reset history must share one transaction");ResetBarrier.View actual=view();if(!actual.known.equals(observed.before.known))throw new IllegalStateException("Reset view changed; review again");if(!observed.resetObserved)return;
  for(String table:new String[]{"tx","budgets","goals","catalog","catalog_meta","preferences","cloud_records","cloud_outbox"})db.delete(table,null,null);for(String optional:new String[]{"sync_outbox","sync_meta","conflict_review","repayment_review"})try(Cursor exists=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",new String[]{optional})){if(exists.moveToFirst())db.delete(optional,null,null);}
  db.delete("reset_history",null,null);for(String id:observed.remote.known){ContentValues row=new ContentValues();row.put("uuid",id);db.insertOrThrow("reset_history",null,row);}
  // Preserve account binding and device ID, invalidate every prior merge snapshot.
  String revision;try(Cursor c=db.rawQuery("SELECT value FROM cloud_meta WHERE name='revision'",null)){if(!c.moveToFirst())throw new IllegalStateException("Cloud state not initialized");revision=Long.toString(Math.addExact(Long.parseLong(c.getString(0)),1));}ContentValues v=new ContentValues();v.put("value",revision);if(db.update("cloud_meta",v,"name='revision'",null)!=1)throw new IllegalStateException("Cannot advance reset revision");
 }
}
