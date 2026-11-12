package in.aicon.pocket;

import java.io.File;
import java.io.IOException;

/** Keep screenshot evidence outside the repository by default while remaining CI-safe. */
final class QaArtifacts {
 private QaArtifacts(){}
 static File file(String name)throws IOException{
  if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.contains("/")||name.contains("\\"))throw new IllegalArgumentException("Invalid QA artifact name");
  String configured=System.getProperty("hisab.qa.dir");
  File directory=configured==null||configured.trim().isEmpty()
   ?new File(System.getProperty("java.io.tmpdir"),"hisab-v2/qa")
   :new File(configured);
  if(!directory.isDirectory()&&!directory.mkdirs()&&!directory.isDirectory())throw new IOException("Could not create QA artifact directory: "+directory);
  return new File(directory,name);
 }
}
