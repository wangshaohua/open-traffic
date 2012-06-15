package netconfig_extensions.io
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

class JsonStringDataSink(ostream: OutputStream) extends DataSink[String] {
  // Constructor logic.
  {
    ostream.write(JsonConsts.header.getBytes())
  }

  def put(n: String) = {
    ostream.write(n.getBytes())
    ostream.write(JsonConsts.sep.getBytes())
  }

  def close(): Unit = {
    ostream.write(JsonConsts.end.getBytes())
    ostream.close()
  }
}

class MappedDataSink[-U, V](sink: DataSink[V], fun: U=>V) extends DataSink[U] {
  def put(u:U) = sink.put(fun(u))
  
  def close() = sink.close()
}

object DataSinks {
  def map[U,V](sink:DataSink[V], fun: U=>V) = new MappedDataSink(sink, fun)
}

object JsonStringDataSinkFactory {
  def openZipped(fname: String) = {
    val ostream = new GZIPOutputStream(new FileOutputStream(fname))
    new JsonStringDataSink(ostream)
  }
}