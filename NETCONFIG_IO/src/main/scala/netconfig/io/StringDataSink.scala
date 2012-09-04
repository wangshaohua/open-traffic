/**
 * Copyright 2012. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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

  def writeableFile(fname: String): StringDataSink = {
    // Create the necessary directories.
    val dir_name = fname.split("/").dropRight(1).mkString("/")
    val f = new File(dir_name)
    if (!f.exists()) {
      logInfo("Creating new directory: " + dir_name)
      f.mkdirs()
    }
    val ostream = new FileOutputStream(fname)
    new StringDataSink(ostream)
  }

}
