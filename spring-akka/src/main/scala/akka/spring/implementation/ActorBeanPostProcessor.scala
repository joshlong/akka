package akka.spring.implementation


import akka.japi.Creator
import reflect.BeanProperty
import akka.actor._
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.BeanPostProcessor
import org.apache.commons.logging.LogFactory

class ActorBeanPostProcessor(@BeanProperty var system: ActorSystem) extends BeanPostProcessor with InitializingBean {

  val logger = LogFactory.getLog(getClass)

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  @BeanProperty var props: Props = Props()

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = bean

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    val isActor = bean.getClass.getAnnotation(classOf[akka.spring.Actor]) != null

    if (isActor) {
      val props = this.props.withCreator(new Creator[Actor] {
        def create(): Actor = new DelegatingActor(bean)
      });
      return system.actorOf(props, beanName);
    }

    bean
  }

  def afterPropertiesSet() {
    logger.info("afterPropertiesSet()")
  }

}
