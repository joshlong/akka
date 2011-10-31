package akka.spring


/**
 * scratchpad.
 */

object Experiments  extends App {

  import akka.AkkaApplication
  import akka.config.Configuration

  val aa = new AkkaApplication ( "aName", Configuration.fromResource("config/akka.conf"))

}