package in.aicon.pocket;
import java.util.*;import java.io.IOException;
/** Guarded Drive adapter used by account-session sync and reset flows. Live Google/Drive
 * behavior still requires real-device tests.
 */
public final class DriveBarrierTransport implements ResetCoordinator.Remote {
 private final DriveAppData drive;
 public DriveBarrierTransport(VerifiedDriveAccess access){this(access,()->{});}
 public DriveBarrierTransport(VerifiedDriveAccess access,SyncEngine.Guard guard){drive=new DriveAppData(subject->{guard.requireCurrent();return access.accessToken(subject);},access.subject);}
 @Override public Map<String,String> resetMarkers()throws Exception{Map<String,String> result=new LinkedHashMap<>();for(String id:drive.listIds("3","reset")){if(result.size()>=ResetBarrier.MAX_RESETS)throw new IOException("Too many reset markers");result.put(id,drive.fetch(id).body);}return result;}
 @Override public Map<String,String> operationFiles()throws Exception{if(!drive.listIds().isEmpty())throw new IOException("An older sync format is present. Explicit migration required; no files overwritten.");Map<String,String> result=new LinkedHashMap<>();for(String id:drive.listIds("3","operation"))result.put(id,drive.fetch(id).body);return result;}
 @Override public String reserveFileId()throws Exception{return drive.reserveFileId();}
 @Override public void appendFile(String fileId,String body)throws Exception{ResetWire.Operation op=ResetWire.parseOperation(fileId,body);drive.append(fileId,op.operation.operationId,body,"3","operation");}
 @Override public void deleteOperationFile(String fileId)throws Exception{drive.deleteContentOperation(fileId);}
 /** Called only by the durable, user-confirmed reset coordinator. It requires the
  * stable file ID and marker UUID to be persisted before this request. */
 public void publishReset(String persistedFileId,String persistedResetId)throws Exception{drive.append(persistedFileId,persistedResetId,ResetWire.marker(persistedResetId),"3","reset");}
}
