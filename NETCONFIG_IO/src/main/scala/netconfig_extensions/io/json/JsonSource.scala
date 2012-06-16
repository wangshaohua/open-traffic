package netconfig_extensions.io

import java.io.BufferedReader
import java.io.FileReader
import java.io.Reader
import scala.collection.Iterable
import org.codehaus.jackson.JsonNode
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.io.InputStreamReader
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.map.ObjectMapper

/**
 * Methods to create sequential sources of data.
 */
object JSONSource {
  val jfactory = (new JsonFactory)
  val mapper = new ObjectMapper
  /**
   * Reads a file line by line, skipping the first line and until the marked final line.
   */
  private[this] def source_string(istream_gen: () => Reader): Iterable[String] = {
    val iterable = new LineIterable {
      def get_reader = istream_gen()
    }
    iterable.view
  }

  /**
   * Reads a file as a sequence of coma-separated strings, that starts with [ and ends with 0].
   */
  def strings(istream_gen: () => Reader): Iterable[String] = {
    source_string(istream_gen).map(_.dropRight(1))
  }
  
  private[this] def getIstreamGen(fname:String): (() => Reader) = {
    def istream_gen() = {
      if (fname.endsWith(".gz")) {
        val fileStream = new FileInputStream(fname);
        val gzipStream = new GZIPInputStream(fileStream);
        new InputStreamReader(gzipStream, "UTF-8");
      } else {
        new FileReader(fname)
      }
    }
    istream_gen
  }
  
  /**
   * Reads a file line by line. Returns an iterable that will lazily operate on the 
   * collection.
   */
  def readLines(fname:String): Iterable[String] = {
    JSONSource.strings(getIstreamGen(fname))
  }
  
  /**
   * Reads a file as a sequence of lines. Each line is interpreted as a standalone JSON  data structure.
   * If the file name ends in '.gz', the file will be interepreted as a zipped JSON file.
   */
  def readFlow(fname: String): Iterable[JsonNode] = {
    JSONSource.strings(getIstreamGen(fname)).map(s => {
        val jparser = jfactory.createJsonParser(s)
        mapper.readTree(jparser)
      })
  }  
}

class LineIterator(private[this] val reader: Reader) extends Iterator[String] {
  private[this] val buffered_reader = new BufferedReader(reader)

  // Constructor logic
  {
    // Skip the first line
    buffered_reader.readLine()
  }

  private[this] var current_line: String = buffered_reader.readLine()

  def hasNext(): Boolean = {
    val res = current_line != null && current_line != JsonConsts.end
    if (!res) {
      buffered_reader.close()
    }
    res
  }

  def next(): String = {
    val line = current_line
    current_line = buffered_reader.readLine()
    line
  }
}

abstract class LineIterable extends Iterable[String] {
  def get_reader: Reader
  def iterator = new LineIterator(get_reader)
}
