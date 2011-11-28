package akka.spring.config

import org.springframework.beans.factory.config._
import javax.annotation.PostConstruct
import akka.actor.{Actor, ActorSystem}
import java.lang.reflect.Method
import org.springframework.util.{Assert, ReflectionUtils}
import org.springframework.beans.factory.InitializingBean

class ActorBeanPostProcessor(val actorSystem: ActorSystem) extends BeanPostProcessor with InitializingBean {

  def log(msg: String) {
    Console.println("the string s " + msg)
  }

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = {
    log("postProcessAfterInitialization(bean, '" + beanName + "')")
    if (bean.isInstanceOf[Actor]) {
      val actorRef = bean.asInstanceOf[Actor]
      val actor = actorSystem.actorOf(actorRef)
      return actor
    }
    bean
  }


  def postProcessBeforeInitialization(bean: AnyRef, p2: String): AnyRef = {
    log("postProcessAfterInitialization " + p2)
    bean
  }

  def afterPropertiesSet() {
    log( "afterPropertiesSet()")
  }
}


/**
 * wrapper calss for the Actor.
 *
 */
  class DelegatingActor(d: AnyRef) extends Actor {

    private[this] def setup(b: AnyRef) {
      val classOfDelegate = delegate.getClass
      val callback = new HandlerResolvingMethodCallback
      val filter = new HandlerResolvingMethodFilter
      ReflectionUtils.doWithMethods(classOfDelegate, callback, filter)
    }

    // provided by logic in #setup

    var receiveMethod: Method = null;
    val delegate = d
    setup(delegate)

    // mark the class as a receive callback
    class HandlerResolvingMethodCallback extends ReflectionUtils.MethodCallback {
      def doWith(m: Method) {
        // find the method
        Assert.isTrue(receiveMethod == null,
          "this indicates that there are two methods " +
            "annotate with @Receive on the class (please specify only one)");
        receiveMethod = m
      }
    }

    // find the receive handling methods on the class
    class HandlerResolvingMethodFilter extends ReflectionUtils.MethodFilter {
      def matches(method: Method) = {
        val annotationType = classOf[akka.spring.Receive]
        method.getAnnotation(annotationType) != null
      }
    }

    protected def doReceive( msg:AnyRef){
      val ar = msg.asInstanceOf[AnyRef];
      receiveMethod.invoke(delegate, ar)
    }

    protected def receive = {
      case e:AnyRef => doReceive(e)
    }
  }