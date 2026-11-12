package in.aicon.pocket;

import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;

/** Drive appDataFolder HTTP transport used through DriveBarrierTransport by account
 * sessions. It uses immutable operation files rather than relying on Drive-v3 compare-and-
 * swap semantics. Sync callers bind requests to the verified Google subject, fetch before
 * uploading, merge vector clocks, persist outbox IDs before sending, and only clean up
 * content files after causal deletion review. Live Google/Drive behavior still requires
 * real-device testing.
 */
public final class DriveAppData {
 public interface Access {String accessToken(String googleSubject)throws Exception;}
 public static class NeedsSignIn extends IOException {public NeedsSignIn(){super("Google authorization required");}}
 public static class RetryLater extends IOException {public RetryLater(){super("Drive unavailable or rate limited");}}
 public static final class Remote {public final String id,body;Remote(String id,String body){this.id=id;this.body=body;}}
 private final Access access;private final String subject;
 public DriveAppData(Access access,String googleSubject){if(googleSubject==null||googleSubject.isEmpty())throw new IllegalArgumentException("Account required");this.access=access;this.subject=googleSubject;}
 private HttpURLConnection connection(String url,String method)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+access.accessToken(subject));return c;}
 private static void check(int status)throws IOException{if(status==401)throw new NeedsSignIn();if(status==429||status>=500)throw new RetryLater();if(status==403)throw new IOException("Drive permission or quota denied; inspect reason before retrying");if(status<200||status>=300)throw new IOException("Drive request failed ("+status+")");}
 private static String read(InputStream in)throws IOException{try(InputStream input=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=input.read(buf))!=-1){if(b.size()+n>8*1024*1024)throw new IOException("Drive sync file exceeds safety limit");b.write(buf,0,n);}return b.toString("UTF-8");}}
 public List<String> listIds()throws Exception{return listIds("2",null);}
 public List<String> listIds(String schema,String kind)throws Exception{if(!Arrays.asList("2","3").contains(schema)||kind!=null&&!Arrays.asList("operation","reset").contains(kind))throw new IllegalArgumentException("Invalid app-data format");List<String> ids=new ArrayList<>();String page="";do{String query="appProperties has { key='hisabSchema' and value='"+schema+"' } and trashed = false"+(kind==null?"":" and appProperties has { key='hisabKind' and value='"+kind+"' }");String url="https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q="+URLEncoder.encode(query,"UTF-8")+"&fields=files(id),nextPageToken&pageSize=1000"+(page.isEmpty()?"":"&pageToken="+URLEncoder.encode(page,"UTF-8"));HttpURLConnection c=connection(url,"GET");try{check(c.getResponseCode());JSONObject j=new JSONObject(read(c.getInputStream()));JSONArray a=j.getJSONArray("files");for(int i=0;i<a.length();i++)ids.add(a.getJSONObject(i).getString("id"));page=j.optString("nextPageToken","");if(ids.size()>100000)throw new IOException("Too many sync operations; needs reviewed compaction");}finally{c.disconnect();}}while(!page.isEmpty());return ids;}
 public Remote fetch(String id)throws Exception{safeId(id);HttpURLConnection c=connection("https://www.googleapis.com/drive/v3/files/"+id+"?alt=media","GET");try{check(c.getResponseCode());return new Remote(id,read(c.getInputStream()));}finally{c.disconnect();}}
 /** Generate once and persist alongside the outbox operation BEFORE sending create. */
 public String reserveFileId()throws Exception{HttpURLConnection c=connection("https://www.googleapis.com/drive/v3/files/generateIds?count=1&space=appDataFolder&type=files","GET");try{check(c.getResponseCode());return new JSONObject(read(c.getInputStream())).getJSONArray("ids").getString(0);}finally{c.disconnect();}}
 /** Idempotent append: stable pre-generated file ID, no mutable shared snapshot. */
 public void append(String fileId,String operationUuid,String body,String schema,String kind)throws Exception{if(!schema.equals("3")||!Arrays.asList("operation","reset").contains(kind))throw new IllegalArgumentException("Invalid app-data file type");
  safeId(fileId);Identifiers.uuid(operationUuid);String boundary="hisab-"+UUID.randomUUID();JSONObject meta=new JSONObject().put("id",fileId).put("name","hisab-op-"+operationUuid+".json").put("parents",new JSONArray().put("appDataFolder")).put("appProperties",new JSONObject().put("hisabSchema",schema).put("hisabKind",kind));String multipart="--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+meta+"\r\n--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+body+"\r\n--"+boundary+"--\r\n";HttpURLConnection c=connection("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id","POST");try{write(c,"multipart/related; boundary="+boundary,multipart);int status=c.getResponseCode();if(status==409){Remote old=fetch(fileId);if(!old.body.equals(body))throw new IOException("Operation ID collision; no data overwritten");return;}check(status);String created=new JSONObject(read(c.getInputStream())).getString("id");if(!created.equals(fileId))throw new IOException("Unexpected created file ID");}finally{c.disconnect();}}
 /** Delete only an operation file already inspected and selected by the causal merge.
  * Must not delete tombstones while any offline replica could still hold prior content.
  */
 public void deleteContentOperation(String id)throws Exception{safeId(id);HttpURLConnection c=connection("https://www.googleapis.com/drive/v3/files/"+id,"DELETE");try{int status=c.getResponseCode();if(status==404)return;check(status);}finally{c.disconnect();}}
 private static void write(HttpURLConnection c,String type,String body)throws IOException{byte[] data=body.getBytes(StandardCharsets.UTF_8);if(data.length>8*1024*1024)throw new IOException("Sync file too large");c.setRequestProperty("Content-Type",type);c.setDoOutput(true);c.setFixedLengthStreamingMode(data.length);try(OutputStream out=c.getOutputStream()){out.write(data);}}
 private static void safeId(String id){if(id==null||!id.matches("[A-Za-z0-9_-]{1,200}"))throw new IllegalArgumentException("Invalid Drive file ID");}
}
