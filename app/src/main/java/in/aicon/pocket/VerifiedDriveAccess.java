package in.aicon.pocket;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Memory-only token boundary used by foreground and background Drive operations.
 * Before Drive work, resolves the token's account directly with Google's HTTPS userinfo
 * endpoint rather than trusting a parsed ID token or assuming the authorization picker
 * used the expected account. Never logs tokens. Callers invalidate pending work before
 * clearing or replacing access.
 */
public final class VerifiedDriveAccess implements DriveAppData.Access {
 public final String subject,email;
 private String token;
 private final long expiresAtNanos;
 private VerifiedDriveAccess(String subject,String email,String token){
  this.subject=subject;this.email=email;this.token=token;
  this.expiresAtNanos=System.nanoTime()+50L*60L*1000000000L;
 }
 /** Run on a worker, never Android's main thread. Scopes must include openid/email
  * as well as drive.appdata. For a reconnect, expectedSubject must be the saved subject.
  * A first connection passes null and requires a separate local-ledger import choice.
  */
 public static VerifiedDriveAccess verify(String accessToken,String expectedSubject)throws Exception {
  if(accessToken==null||accessToken.isEmpty()||accessToken.contains("\r")||accessToken.contains("\n"))throw new IOException("Authorization required");
  HttpURLConnection c=(HttpURLConnection)new URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
  c.setRequestProperty("Authorization","Bearer "+accessToken);
  try {
   if(c.getResponseCode()!=200)throw new IOException("Could not verify the Google account; no sync performed");
   ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>65536)throw new IOException("Unexpected account response");b.write(buf,0,n);}}
   JSONObject j=new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8));String sub=j.getString("sub");AccountScope.databaseFor(sub);
   String email=j.getString("email");if(!j.optBoolean("email_verified",false)||email.length()>320||email.contains("\n")||email.contains("\r"))throw new IOException("Google account email not verified");
   if(expectedSubject!=null&&!expectedSubject.equals(sub))throw new IOException("Google account differs from this ledger; no sync performed");
   return new VerifiedDriveAccess(sub,email,accessToken);
  }finally{c.disconnect();}
 }
 @Override public synchronized String accessToken(String googleSubject)throws Exception {
  if(!subject.equals(googleSubject))throw new IOException("Account mismatch; no Drive request sent");
  if(token==null||System.nanoTime()-expiresAtNanos>=0)throw new DriveAppData.NeedsSignIn();
  return token;
 }
 public synchronized void clear(){token=null;}
 @Override public String toString(){return "VerifiedDriveAccess[redacted]";}
}
