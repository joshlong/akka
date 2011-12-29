package akka.spring

import akka.spring.actors.{Order, ShoppingCart}
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.apache.commons.logging.LogFactory
import akka.actor.ActorRef

object Experiments extends App {

  val logger = LogFactory.getLog(getClass)

  val applicationContext = new AnnotationConfigApplicationContext
  applicationContext.register(classOf[ActorConfiguration])
  applicationContext.addBeanFactoryPostProcessor(new AkkaBeanFactoryPostProcessor)
  applicationContext.refresh()

  val noisyActor = applicationContext.getBean("noisyActor", classOf[ActorRef])
  val shoppingActor = applicationContext.getBean("shoppingActor", classOf[ActorRef])

  val order = Order(24)
  val shoppingCart = ShoppingCart(  List(order) )

  shoppingActor ! shoppingCart
  shoppingActor ! order

  noisyActor ! "Hello, world!"

}