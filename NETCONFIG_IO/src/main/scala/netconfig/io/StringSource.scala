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

import java.io.BufferedReader
import java.io.FileReader
import java.io.Reader
import scala.collection.Iterable
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.io.InputStreamReader

class LineIterator(private[this] val reader: Reader) extends Iterator[String] {
  private[this] val buffered_reader = new BufferedReader(reader)

  // Constructor logic
  //  {
  //    // Skip the first line
  //    buffered_reader.readLine()
  //  }

  private[this] var current_line: String = buffered_reader.readLine()

  def hasNext(): Boolean = {
    val res = current_line != null && !current_line.isEmpty()
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

object StringSource {
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
   * Represents a file as a sequence of lines (strings) on which one can iterate.
   * The files is accessed lazily.
   */
  def strings(istream_gen: () => Reader): Iterable[String] = {
    source_string(istream_gen) //.map(_.dropRight(1))
  }

  private[this] def getIstreamGen(fname: String): (() => Reader) = {
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
  def readLines(fname: String): Iterable[String] = {
    StringSource.strings(getIstreamGen(fname))
  }

}