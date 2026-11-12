package in.aicon.pocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/** Account subject validation and deterministic database-name helper. The Ticket
 * methods remain a standalone boundary model; production session ownership and stale
 * operation checks are enforced by LedgerSession and LedgerController. This class never
 * moves a local ledger as an incidental effect of sign-in.
 */
public final class AccountScope {
 public static final String LOCAL_DATABASE="pocket.db";
 public static final class Ticket {
  public final String subject,database;
  private final long epoch;
  private final Object owner;
  private Ticket(String subject,long epoch,Object owner){this.subject=subject;this.database=databaseFor(subject);this.epoch=epoch;this.owner=owner;}
 }
 private final Object owner=new Object();
 private String subject;
 private long epoch;
 public synchronized void bind(String googleSubject){
  requireSubject(googleSubject);
  // Always invalidate old operations, including reconnects to the same account.
  epoch=Math.addExact(epoch,1L);subject=googleSubject;
 }
 public synchronized void disconnect(){epoch=Math.addExact(epoch,1L);subject=null;}
 public synchronized Ticket ticket(){if(subject==null)throw new IllegalStateException("Google account not connected");return new Ticket(subject,epoch,owner);}
 public synchronized boolean isCurrent(Ticket t){return t!=null&&t.owner==owner&&subject!=null&&epoch==t.epoch&&Objects.equals(subject,t.subject);}
 public synchronized void requireCurrent(Ticket t){if(!isCurrent(t))throw new IllegalStateException("Account changed; discard this result");}
 public static String databaseFor(String subject){
  requireSubject(subject);
  try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder("hisab-google-");for(byte b:digest)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s+".db";}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
 }
 private static void requireSubject(String subject){if(subject==null||!subject.matches("[0-9]{1,255}"))throw new IllegalArgumentException("Expected Google account subject, not email");}
}
