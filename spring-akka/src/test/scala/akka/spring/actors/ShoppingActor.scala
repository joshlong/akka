package akka.spring.actors

import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.logging.LogFactory
import akka.actor.{ActorRef => AR, ActorContext}
import akka.spring._

@akka.spring.Actor("shoppingActor")
class ShoppingActor {

  @Autowired var actorContext: ActorContext = _

  @akka.spring.ActorReference ("noisyActor") var actorRef: akka.actor.ActorRef = _

  val logger = LogFactory.getLog(getClass)

  @Receive
  def handleShoppingCart(@Payload sc: ShoppingCart) {
    logger.info("shopping cart:" + sc.toString)
  }

  @Receive
  def handleOrder(@Self self: AR,
                  @Context ac: ActorContext,
                  @Payload o: Order) {

    logger.info("i can call the ActorContext " + this.actorContext.self.toString())
    logger.info("received an order " + o.toString + " for the 'self' " + self.toString())
  }

}
