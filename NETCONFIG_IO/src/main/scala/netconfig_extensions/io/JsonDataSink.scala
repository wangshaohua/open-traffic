package netconfig_extensions.io
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream
import java.io.File
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import core_extensions.MMLogging

class JsonDataSink(ostream: OutputStream) extends DataSink[JsonNode] {
  val mapper = new ObjectMapper
  val factory = new JsonFactory

  // Constructor logic.
  {
    ostream.write(JsonConsts.header.getBytes())
  }

  lazy val generator = factory.createJsonGenerator(ostream)

  def put(n: JsonNode) = {
    mapper.writeTree(generator, n)
    ostream.write(JsonConsts.sep.getBytes())
  }

  def close(): Unit = {
    ostream.write(JsonConsts.end.getBytes())
    ostream.close()
  }
}

object JsonDataSink extends MMLogging {
  def writeableZippedFile(fname: String):JsonDataSink = {
    // Create the necessary directories.
    val dir_name = fname.split("/").dropRight(1).mkString("/")
    val f = new File(dir_name)
    if (!f.exists()) {
      logInfo("Creating new directory: "+dir_name)
      f.mkdirs()
    }
    val ostream = new GZIPOutputStream(new FileOutputStream(fname))
    new JsonDataSink(ostream)
  }
}

object JsonConsts {
  val header = "[\n"
  val end = "0]"
  val sep = ",\n"
}