package akka.spring.config

import org.springframework.beans.factory.config._

class ActorBeanPostProcessor extends BeanPostProcessor {

  def log (msg:String) {
    System.out.println("the string s " + msg)
  }

  def postProcessAfterInitialization(bean: AnyRef, p2: String): AnyRef = {
    log( "postProcessAfterInitialization "+  p2 +".")
    bean
  }

  def postProcessBeforeInitialization( bean : AnyRef, p2: String ): AnyRef = {
    log( "postProcessAfterInitialization "+ p2)
    bean
  }
}