package akka.spring


import config.util.Log
import config.{DelegatingActorContextFactoryBean, ActorBeanPostProcessor}
import javax.annotation.PostConstruct
import org.springframework.context.annotation.{AnnotationConfigApplicationContext, Configuration, Bean}
import akka.actor.{ActorContext, ActorRef, ActorSystem}
import org.springframework.beans.factory.annotation.Autowired


case class Order(amount: Int)

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

  def spring () {
    val ac = new AnnotationConfigApplicationContext()
    ac.register(classOf[SimpleConfiguration])
    ac.addBeanFactoryPostProcessor( new AkkaBeanFactoryPostProcessor)
    ac.refresh()      
    val actorRef = ac.getBean(classOf[ActorRef])
    actorRef ! Order(24)
  }



  def actorContext()  {
    val acfb = new DelegatingActorContextFactoryBean
    val ac : ActorContext = acfb.getObject 
    Log.log("analysing the created actor context")
    ac.self
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
  var actorContext :ActorContext = _
  
  @Receive
  def handleOrder(@Self self: ActorRef, @Payload o: Order) {
    
    Log.log ("i can call the ActorContext " + this.actorContext.self)
    Log.log("received an order " + o.toString + " for the 'self' " + self.toString())
  }
 
}

@Configuration
class SimpleConfiguration {
  
  @Bean def actorSystem = ActorSystem()

  @Bean def myActor = new MyActor 

}
