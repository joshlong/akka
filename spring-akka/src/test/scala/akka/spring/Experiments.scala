package akka.spring

import config.ActorBeanPostProcessor
import org.springframework.context.annotation.{Configuration, Bean, AnnotationConfigApplicationContext}
import javax.annotation.PostConstruct


/**
 * scratchpad.
 */
object Experiments extends App {

  //  val aa = new AkkaApplication ( "aName", Configuration.fromResource("config/akka.conf"))

  val ac = new AnnotationConfigApplicationContext(classOf[SimpleConfiguration])
}

@Actor
class MyActor {

  @PostConstruct
  def setup() {
    Console.printf("PostConstruct...")
  }

}

@Configuration
class SimpleConfiguration {
  @Bean
  def actorBpp() =
    new ActorBeanPostProcessor
}