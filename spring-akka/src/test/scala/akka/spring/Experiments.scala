package akka.spring


import akka.actor.{ActorRef, ActorSystem}

import config.ActorBeanPostProcessor
import javax.annotation.PostConstruct
import org.springframework.context.annotation.{AnnotationConfigApplicationContext, Configuration, Bean}


case class Order(amount: Int)

object Experiments extends App {

  val system = ActorSystem()

  val bfpp = new ActorBeanPostProcessor

  val bn = "myactor";

  var actor: AnyRef = new MyActor
  actor = bfpp.postProcessBeforeInitialization(actor, bn)
  actor = bfpp.postProcessAfterInitialization(actor, bn)

  val ma :ActorRef =  actor.asInstanceOf[ActorRef];
  ma ! Order(242)


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
  def aCustomReceiveMethod(e:AnyRef) : akka.actor.Actor.Receive = {
    case Order(amount) ⇒ Console.println("Order received!");
    case e ⇒ Console.println("something else received")
  }
}

@Configuration
class SimpleConfiguration {

  @Bean def actorBpp = new ActorBeanPostProcessor()

}
