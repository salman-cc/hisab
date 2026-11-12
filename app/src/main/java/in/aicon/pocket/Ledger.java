package in.aicon.pocket;
import java.math.*; import java.util.*;
public final class Ledger {
 public static long amount(String s) { if(s==null||!s.trim().matches("[0-9]{1,10}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Enter an amount with up to 2 decimal places."); long n=new BigDecimal(s.trim()).movePointRight(2).longValueExact();if(n<=0||n>999999999999L)throw new IllegalArgumentException("Enter an amount above zero."); return n; }
 public static String number(long n){return BigDecimal.valueOf(n,2).setScale(2).toPlainString();}
 public static long[] split(long total,int people){if(total<=0||people<2||people>100)throw new IllegalArgumentException();long[] a=new long[people];for(int i=0;i<people;i++)a[i]=total/people+(i<total%people?1:0);return a;}
 public static long effect(String type,long amount,String from,String to,String account){if((type.equals("income")||type.equals("borrowed")||type.equals("repay_in")))return from.equals(account)?amount:0;if((type.equals("expense")||type.equals("lent")||type.equals("repay_out")))return from.equals(account)?-amount:0;if(type.equals("transfer"))return(from.equals(account)?-amount:0)+(to.equals(account)?amount:0);throw new IllegalArgumentException();}
 public static String csv(String s){if(s==null)s="";boolean control=!s.isEmpty()&&(s.charAt(0)=='\t'||s.charAt(0)=='\r'||s.charAt(0)=='\n');int first=0;while(first<s.length()&&(Character.isWhitespace(s.charAt(first))||Character.isSpaceChar(s.charAt(first))||s.charAt(first)=='\ufeff'))first++;if(control||first<s.length()&&"=+-@".indexOf(s.charAt(first))>=0)s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
