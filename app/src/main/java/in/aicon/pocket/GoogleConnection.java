package in.aicon.pocket;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.CancellationSignal;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;
import com.google.android.libraries.identity.googleid.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Optional Google connection flow. The Credential Manager result selects an account;
 * the ID token is never parsed/trusted or sent to an AiCoN backend. The access token
 * is verified against Google's HTTPS userinfo response before any ledger is selected.
 * All public methods must be called on the main thread. A stopped Activity must close.
 */
public final class GoogleConnection implements AutoCloseable {
 public interface Listener {
  void connected(VerifiedDriveAccess access);
  void stopped(String message);
 }
 public static final int AUTHORIZATION_REQUEST=18741;
 private final Activity activity;
 private final Listener listener;
 private final CredentialManager credentials;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private CancellationSignal cancellation;
 private long generation;
 private boolean active,closed;
 private String selectedEmail,expectedSubject;
 private static final List<Scope> SCOPES=Collections.unmodifiableList(Arrays.asList(
  new Scope(CloudConfig.DRIVE_SCOPE),new Scope("openid"),
  new Scope("https://www.googleapis.com/auth/userinfo.email"),
  new Scope("https://www.googleapis.com/auth/userinfo.profile")));
 public GoogleConnection(Activity activity,Listener listener){this.activity=activity;this.listener=listener;credentials=CredentialManager.create(activity);}
 private static void main(){if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper())throw new IllegalStateException("Main thread required");}
 private boolean current(long g){return !closed&&active&&generation==g&&!activity.isFinishing()&&!activity.isDestroyed();}
 /** User taps Sign in with Google. Existing subject supplied only for reconnect;
  * do not adopt an unrelated ledger or upload local data in the listener.
  */
 public void start(String reconnectSubject){
  main();if(closed||active)return;expectedSubject=reconnectSubject;
  if(reconnectSubject!=null)AccountScope.databaseFor(reconnectSubject);
  active=true;long g=++generation;cancellation=new CancellationSignal();
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
  String nonce=android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE|android.util.Base64.NO_PADDING);
  GetSignInWithGoogleOption option=new GetSignInWithGoogleOption.Builder(CloudConfig.WEB_CLIENT_ID).setNonce(nonce).build();
  GetCredentialRequest request=new GetCredentialRequest.Builder().addCredentialOption(option).build();
  credentials.getCredentialAsync(activity,request,cancellation,activity::runOnUiThread,
   new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
    @Override public void onResult(GetCredentialResponse result){
     if(!current(g))return;
     try {
      Credential value=result.getCredential();
      if(!(value instanceof CustomCredential)||!(value.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)||value.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)))throw new IllegalArgumentException();
      GoogleIdTokenCredential account=GoogleIdTokenCredential.createFrom(value.getData());
      selectedEmail=account.getEmail();
      if(selectedEmail==null||!selectedEmail.contains("@")||selectedEmail.length()>320)throw new IllegalArgumentException();
      authorize(g);
     }catch(Exception e){stop(g,"Google sign-in did not return a usable account. No sync performed.");}
    }
    @Override public void onError(GetCredentialException e){stop(g,"Google sign-in was not completed. Your local data is unchanged.");}
   });
 }
 private void authorize(long g){
  if(!current(g))return;
  AuthorizationRequest request=AuthorizationRequest.builder().setAccount(new Account(selectedEmail,"com.google")).setRequestedScopes(SCOPES).build();
  Identity.getAuthorizationClient(activity).authorize(request).addOnSuccessListener(activity,result->{
   if(!current(g))return;
   if(result.hasResolution()){
    try{activity.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),AUTHORIZATION_REQUEST,null,0,0,0);}
    catch(Exception e){stop(g,"Google permission screen could not open. No sync performed.");}
   }else accept(result,g);
  }).addOnFailureListener(activity,e->stop(g,"Google Drive permission could not be obtained. Your data is unchanged."));
 }
 /** Forward Activity.onActivityResult. A result after process death cannot resume a
  * previous transaction; explicit sign-in must begin again with fresh account checks.
  */
 public boolean onActivityResult(int requestCode,int resultCode,Intent data){
  main();if(requestCode!=AUTHORIZATION_REQUEST)return false;
  long g=generation;if(!current(g))return true;
  if(resultCode!=Activity.RESULT_OK||data==null){stop(g,"Google permission was not granted. Your local data is unchanged.");return true;}
  try{accept(Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data),g);}
  catch(Exception e){stop(g,"Google authorization could not be read. No sync performed.");}
  return true;
 }
 private void accept(AuthorizationResult result,long g){
  if(!current(g))return;
  if(!result.getGrantedScopes().contains(CloudConfig.DRIVE_SCOPE)||!result.getGrantedScopes().contains("openid")||!result.getGrantedScopes().contains("https://www.googleapis.com/auth/userinfo.email")){
   stop(g,"Hisab needs its own Drive app-data permission to sync. Your local data is unchanged.");return;
  }
  final String token=result.getAccessToken(),email=selectedEmail,subject=expectedSubject;
  worker.execute(()->{
   VerifiedDriveAccess access=null;
   try{
    access=VerifiedDriveAccess.verify(token,subject);
    if(!email.equalsIgnoreCase(access.email)){access.clear();throw new IllegalStateException("Account differs");}
    final VerifiedDriveAccess verified=access;
    activity.runOnUiThread(()->{if(!current(g)){verified.clear();return;}active=false;selectedEmail=null;expectedSubject=null;listener.connected(verified);});
   }catch(Exception e){if(access!=null)access.clear();activity.runOnUiThread(()->stop(g,"Google account verification failed. No ledger was connected or uploaded."));}
  });
 }
 private void stop(long g,String message){if(!current(g))return;active=false;selectedEmail=null;expectedSubject=null;listener.stopped(message);}
 /** Credential-manager account-choice clearing is separate from cloud deletion.
  * Callers must stop selected WorkManager work and invalidate the matching session first.
  */
 public void clearAccountChoice(Runnable complete){
  main();cancel();if(closed)return;
  credentials.clearCredentialStateAsync(new ClearCredentialStateRequest(),null,activity::runOnUiThread,new CredentialManagerCallback<Void,ClearCredentialException>(){
   @Override public void onResult(Void result){if(!closed)complete.run();}
   @Override public void onError(ClearCredentialException e){if(!closed)listener.stopped("Local connection closed, but Google account-choice state could not be cleared. Try sign-in again.");}
  });
 }
 public void cancel(){main();generation++;active=false;selectedEmail=null;expectedSubject=null;if(cancellation!=null)cancellation.cancel();cancellation=null;}
 @Override public void close(){main();cancel();closed=true;worker.shutdownNow();}
}
