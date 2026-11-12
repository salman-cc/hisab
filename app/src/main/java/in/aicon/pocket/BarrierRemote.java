package in.aicon.pocket;
import java.util.*;

/** Reset-aware operation transport used by LedgerSession with DriveBarrierTransport.
 * A new barrier stops the run before local merge/upload. An in-flight append from before
 * a reset remains tagged with its old view and is filtered out on the next pull.
 */
public final class BarrierRemote implements SyncEngine.Remote {
 public interface Transport {
  Map<String,String> resetMarkers()throws Exception;
  Map<String,String> operationFiles()throws Exception;
  String reserveFileId()throws Exception;
  void appendFile(String fileId,String body)throws Exception;
  void deleteOperationFile(String fileId)throws Exception;
 }
 public static final class ResetObserved extends Exception {public final ResetBarrier.Check check;ResetObserved(ResetBarrier.Check check){super("Cloud data was reset. Review this ledger before reconnecting.");this.check=check;}}
 private final Transport transport;private final ResetBarrier.View view;private final Set<String> inspected=new HashSet<>();
 public BarrierRemote(Transport transport,ResetBarrier.View view){this.transport=Objects.requireNonNull(transport);this.view=Objects.requireNonNull(view);}
 private ResetBarrier.Check check()throws Exception{Map<String,String> files=transport.resetMarkers();if(files.size()>ResetBarrier.MAX_RESETS)throw new IllegalArgumentException("Too many reset files");Set<String> ids=new HashSet<>();for(String body:files.values())ids.add(ResetWire.parseMarker(body));ResetBarrier.Check check=view.inspect(ids);if(check.resetObserved)throw new ResetObserved(check);return check;}
 @Override public List<SyncEngine.RemoteOperation> pull()throws Exception{ResetBarrier.Check check=check();Map<String,String> files=transport.operationFiles();if(files.size()>100000)throw new IllegalArgumentException("Too many operation files");List<SyncEngine.RemoteOperation> result=new ArrayList<>();Set<String> operationIds=new HashSet<>();Set<String> current=new HashSet<>();for(Map.Entry<String,String> file:files.entrySet()){ResetWire.Operation parsed=ResetWire.parseOperation(file.getKey(),file.getValue());if(!check.accepts(parsed.resets))continue;if(!operationIds.add(parsed.operation.operationId))throw new IllegalArgumentException("Duplicate remote operation ID");result.add(parsed.operation);current.add(file.getKey());}check();synchronized(inspected){inspected.clear();inspected.addAll(current);}return result;}
 @Override public String reserveFileId()throws Exception{check();return transport.reserveFileId();}
 @Override public void append(SyncEngine.Pending operation)throws Exception{if(operation.fileId==null)throw new IllegalArgumentException("Persist file ID before upload");check();transport.appendFile(operation.fileId,ResetWire.operation(operation,view));}
 @Override public void deleteContentFile(String id)throws Exception{check();synchronized(inspected){if(!inspected.contains(id))throw new IllegalArgumentException("Can delete only an inspected current operation");}transport.deleteOperationFile(id);}
 @Override public int cleanupRetired()throws Exception{ResetBarrier.Check current=check();Map<String,String> files=transport.operationFiles();if(files.size()>100000)throw new IllegalArgumentException("Too many operation files");int removed=0;for(Map.Entry<String,String> file:files.entrySet()){ResetWire.Operation operation=ResetWire.parseOperation(file.getKey(),file.getValue());if(current.accepts(operation.resets))continue;check();transport.deleteOperationFile(file.getKey());removed++;}return removed;}

}
