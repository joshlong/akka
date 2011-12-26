package akka.spring.config


import akka.spring.config.util.Log.log

import akka.japi.Creator
import reflect.BeanProperty
import akka.actor._
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.BeanPostProcessor


/**
 *
 * A simple Spring BeanPostProcessor that finds all beans that have the Actor stereotype annotation
 * and inspects them for particular handler methods.
 *
 * TOOD it would be ideal if this could be built on top of the typed actor implementation.
 *
 */
class ActorBeanPostProcessor extends BeanPostProcessor with InitializingBean {

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  //todo replace this with the ActorSystem from the ActorSystemFactoryBean
  @BeanProperty var system: ActorSystem = ActorSystem()

  @BeanProperty var props: Props = Props()

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = {
    bean
  }

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    val isActor = bean.getClass.getAnnotation(classOf[akka.spring.Actor]) != null

    if (isActor) {
      val createdActor = this.props.withCreator(new Creator[Actor] {
        def create(): Actor = new DelegatingActor(bean)
      });
      return system.actorOf(createdActor);
    }

    bean
  }

  def afterPropertiesSet() {
    log("afterPropertiesSet()")
  }
}
