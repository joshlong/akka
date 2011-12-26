package akka.spring.config.util

import org.apache.log4j.Logger


object Log {
  def log(a: String) {
    Logger.getLogger(getClass).info(a)
  }
}
