package akka.spring

import akka.actor.{ ActorRef, Actor, ActorSystem }

import config.{DelegatingActor, ActorBeanPostProcessor}
import javax.annotation.PostConstruct
import org.springframework.context.annotation.{Configuration, Bean}

case class Order( amount: Int )

object Experiments extends App {
  val bfpp = new ActorBeanPostProcessor()
  bfpp.afterPropertiesSet()

  val system = ActorSystem()
  val myActor = new MyActor()

  val actorRef = bfpp.postProcessBeforeInitialization( myActor, "myActor").asInstanceOf[ActorRef]
  actorRef ! Order(242)

  ///val delegatingActor = bfpp.postProcessBeforeInitialization( myActor, "myActor").asInstanceOf[akka.actor.ActorRef]
  /*  val applicationContext = new AnnotationConfigApplicationContext
  applicationContext.scan(classOf[MyActor].getPackage.getName)
  applicationContext.refresh()*/
}

@akka.spring.Actor
class MyActor {

  var system = ActorSystem()

  @PostConstruct
  def setup() {
    Console.println("PostConstruct(): actorSystem is " +
      (if (this.system != null) "not" else "") + " null");
  }

  // protected def receive:Receive =>
  @Receive
  protected def aCustomReceiveMethod(msg: Any): akka.actor.Actor.Receive = {
    case Order(amount) ⇒ Console.println("Order received!");
    case e   ⇒ Console.println( "something else received")
  }
}

@Configuration
class SimpleConfiguration {

  @Bean def actorBpp = new ActorBeanPostProcessor()

}
