package akka.spring.actors

import akka.spring.{Context, Self, Payload, Receive}
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.logging.LogFactory
import akka.actor.{ActorRef, ActorContext}

@akka.spring.Actor("shoppingActor")
class ShoppingActor {

  @Autowired var actorContext: ActorContext = _

  val logger = LogFactory.getLog(getClass)

  @Receive
  def handleShoppingCart(@Payload sc: ShoppingCart) {
    logger.info("shopping cart:" + sc.toString)
  }

  @Receive
  def handleOrder(@Self self: ActorRef,
                  @Context ac: ActorContext,
                  @Payload o: Order) {

    logger.info("i can call the ActorContext " + this.actorContext.self.toString())
    logger.info("received an order " + o.toString + " for the 'self' " + self.toString())
  }

}
