package akka.spring

import akka.spring.config.util.Log._
import config.util.Log
import org.springframework.context.annotation.{AnnotationConfigApplicationContext, Configuration, Bean}
import akka.actor.{ActorContext, ActorRef, ActorSystem}
import org.springframework.beans.factory.annotation.Autowired




object Experiments extends App {

  /* val system = ActorSystem()
  
    val bfpp = new ActorBeanPostProcessor
  
    val bn = "myactor";
  
    var actor: AnyRef = new MyActor
    actor = bfpp.postProcessBeforeInitialization(actor, bn)
    actor = bfpp.postProcessAfterInitialization(actor, bn)
  
    val ma: ActorRef = actor.asInstanceOf[ActorRef];
    ma ! Order(242)
  */

  def spring() {
    val ac = new AnnotationConfigApplicationContext()
    ac.register(classOf[SimpleConfiguration])
    ac.addBeanFactoryPostProcessor(new AkkaBeanFactoryPostProcessor)
    ac.refresh()

    val actorRef = ac.getBean(classOf[ActorRef])

    val o = Order(24)
    val sc = ShoppingCart(List(o))

    actorRef ! sc
    actorRef ! o

  }

  spring()

  ///val delegatingActor = bfpp.postProcessBeforeInitialization( myActor, "myActor").asInstanceOf[akka.actor.ActorRef]
  /*  val applicationContext = new AnnotationConfigApplicationContext
  applicationContext.scan(classOf[MyActor].getPackage.getName)
  applicationContext.refresh()*/
}

@akka.spring.Actor
class MyActor {

  @Autowired
  var actorContext: ActorContext = _

  @Receive
  def handleShoppingCart (@Payload sc:ShoppingCart){
    Log.log("shopping cart:"+ sc.toString)
  }
  
  @Receive
  def handleOrder(@Self self: ActorRef,
                  @Context ac: ActorContext,
                  @Payload o: Order) {

    Log.log("i can call the ActorContext " + this.actorContext.self.toString())
    Log.log("received an order " + o.toString + " for the 'self' " + self.toString())
  }

}

@Configuration
class SimpleConfiguration {

  @Bean def actorSystem = ActorSystem()

  @Bean def myActor = new MyActor

}

case class Order(amount: Int) 

case class ShoppingCart(orders: List[Order])

/*
object Free {
  val shoppingCart = new ShoppingCart
  shoppingCart.orders =  { new Order(242) :: shoppingCart.orders };
}*/
