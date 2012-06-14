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
package core_extensions

/**
 * Trait that encapsulates the logging messages you want to send from scala
 * in a convenient interface.
 *
 * Inherit this trait to print some statements.
 *
 * This is useful when working in a distributed environment as it can properly
 * encapsulate the messages.
 */
trait MMLogging {
  def logInfo(msg: => String): Unit = {
    MMLogging.info(msg)
  }

  def logWarning(msg: => String): Unit = {
    MMLogging.info(msg)
  }

  def logError(msg: => String): Unit = {
    MMLogging.err(msg)
  }

  def checkAssert(assertion: => Boolean, msg: => String): Unit = {
    val ass_res = assertion
    if (!ass_res) {
      logError(msg)
    }
    assert(ass_res)
  }

  def logWarning(msg: => String, e: Throwable): Unit = {
    MMLogging.info(msg)
    MMLogging.info(e)
  }

  def logError(msg: => String, e: Throwable): Unit = {
    MMLogging.err(msg)
    MMLogging.err(e)
  }

}

object MMLogging {
  // scalastyle:off
  def err(x: Any) = { java.lang.System.err.println(x) }
  def out(x: Any) = { java.lang.System.out.println(x) }
  def info(x: Any) = { java.lang.System.out.println(x) }
  // scalastyle:on
}
