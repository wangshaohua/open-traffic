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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Trait that encapsulates the logging messages you want to send from scala
 * in a convenient interface.
 *
 * Inherit this trait to print some statements.
 *
 * This is useful when working in a distributed environment as it can properly
 * encapsulate the messages.
 * Uses LOG4J as a backend.
 */
trait MMLogging {

   // TODO(?) this construction of a logger may not be thread-safe. To check!
  @transient
  lazy val log: Logger = {
    val name = {
      val className = this.getClass().getName()
      // Ignore trailing $'s in the class names for Scala objects
      if (className.endsWith("$")) {
        className.substring(0, className.length - 1)
      } else className
    }
    LoggerFactory.getLogger(name)
  }

  def logDebug(msg: => String): Unit = {
    if (log.isDebugEnabled) log.debug(msg)
  }

  def logInfo(msg: => String): Unit = {
    if (log.isInfoEnabled) log.info(msg)
  }

  def logWarning(msg: => String): Unit = {
    if (log.isWarnEnabled) log.warn(msg)
  }

  def logError(msg: => String): Unit = {
    if (log.isErrorEnabled) log.error(msg)
  }

  def checkAssert(assertion: => Boolean, msg: => String): Unit = {
    val ass_res = assertion
    if (!ass_res) {
      logError(msg)
    }
    assert(ass_res)
  }

  def logWarning(msg: => String, e: Throwable): Unit = {
    if (log.isWarnEnabled) log.warn(msg)
  }

  def logError(msg: => String, e: Throwable): Unit = {
    if (log.isErrorEnabled) log.error(msg, e)
  }

}
