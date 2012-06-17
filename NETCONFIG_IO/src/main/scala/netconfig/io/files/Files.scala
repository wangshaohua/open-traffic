package netconfig.io.files
import scala.sys.SystemProperties

object Files {
  def dataDir() = {
    val s = new SystemProperties
    s.get("mm.data.dir") match {
      case Some(s) => s
      case _ => "/tmp/"
    }
  }

  def setDataDir(s: String): Unit = {
    val sp = new SystemProperties
    sp.put("mm.data.dir", s)
  }

}