package akka.spring.actors
import akka.actor.{ActorSystem, ActorContext}
import akka.spring.{Receive, Payload}
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired

@akka.spring.Actor("noisyActor")
class NoisyActor extends InitializingBean {

  val logger = LogFactory.getLog(getClass)

  @Autowired var context: ActorContext = _
  @Autowired var actorSystem: ActorSystem = _

  @Receive
  def makeNoise(@Payload whatToSay: String) {
    System.out.println("saying: " + whatToSay)
  }

  def afterPropertiesSet() {
    logger.info("actorSystem = " + this.actorSystem + "; context = " + this.context)
  }
}
