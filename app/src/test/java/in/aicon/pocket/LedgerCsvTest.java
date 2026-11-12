package in.aicon.pocket;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LedgerCsvTest {
 @Test public void formulaPrefixesAndLeadingControlsAreEscapedAsText(){
  String[] formulas={"=1+1","+1","-1","@SUM(1,2)","\t=1+1","\r=1+1","\n=1+1","  =1+1","\ufeff=1+1"};
  for(String value:formulas)assertEquals("unsafe CSV prefix: "+value,"\"'"+value.replace("\"","\"\"")+"\"",Ledger.csv(value));
  for(String control:new String[]{"\t","\r","\n"})assertEquals("\"'"+control+"\"",Ledger.csv(control));
  assertEquals("\"O\"\"Brien\"",Ledger.csv("O\"Brien"));
  assertEquals("\"12.30\"",Ledger.csv("12.30"));
  assertEquals("\"\"",Ledger.csv(null));
 }
}
