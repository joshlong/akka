package akka.spring


import akka.spring.config.ActorBeanPostProcessor
import javax.annotation.PostConstruct
import org.springframework.context.annotation.{Configuration, Bean, AnnotationConfigApplicationContext}
import akka.actor.ActorSystem
import org.springframework.beans.factory.annotation.Autowired


/**
 * scratchpad.
 */
object Experiments extends App {
  val applicationContext = new AnnotationConfigApplicationContext
  applicationContext.scan(classOf[MyActor].getPackage.getName)
  applicationContext.refresh()
}

@akka.spring.Actor
class MyActor {

  @Autowired
  var actorSystem: ActorSystem = null;

  @PostConstruct
  def setup() {

    Console.println("PostConstruct(): actorSystem is " +
      (if (this.actorSystem != null) "not" else "") + " null");

  }

  // protected def receive:Receive =>
  @Receive
  protected def aCustomReceiveMethod(msg: Any) {
    Console.println("received a notification")
  }
}

@Configuration
class SimpleConfiguration {

  @Bean def actorSystem = ActorSystem()

  @Bean def actorBpp() = new ActorBeanPostProcessor(actorSystem)
}
