package netconfig.io
import java.io.OutputStream
import core_extensions.MMLogging
import java.io.File
import java.util.zip.GZIPOutputStream
import java.io.FileOutputStream

class StringDataSink(
  ostream: OutputStream,
  separator: String = "\n") extends DataSink[String] {
  def put(n: String) = {
    ostream.write(n.getBytes())
    ostream.write(separator.getBytes())
  }

  def close(): Unit = {
    //    ostream.write(JsonConsts.end.getBytes())
    ostream.close()
  }
}

object StringDataSink extends MMLogging {
  def writeableZippedFile(fname: String): StringDataSink = {
    // Create the necessary directories.
    val dir_name = fname.split("/").dropRight(1).mkString("/")
    val f = new File(dir_name)
    if (!f.exists()) {
      logInfo("Creating new directory: " + dir_name)
      f.mkdirs()
    }
    val ostream = new GZIPOutputStream(new FileOutputStream(fname))
    new StringDataSink(ostream)
  }
}
