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
package edu.berkeley.path.bots.netconfig.io
/**
 * Objects that store data sequentially.
 *
 * @author tjhunter
 *
 * @type DatumType: the type of datum that you can store in this collection.
 */
trait DataSink[-DatumType] {

  /**
   * Inserts the datum in the sink.
   */
  def put(datum: DatumType)

  /**
   * Indicates that no other datum will be pushed in the sink, so that
   * optional closing operations may be performed.
   */
  def close(): Unit
}

class MappedDataSink[-U, V](sink: DataSink[V], fun: U => V) extends DataSink[U] {
  def put(u: U) = sink.put(fun(u))

  def close() = sink.close()
}

object DataSinks {
  def map[U, V](sink: DataSink[V], fun: U => V) = new MappedDataSink(sink, fun)
}
