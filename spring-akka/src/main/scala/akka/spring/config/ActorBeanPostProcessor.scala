package akka.spring.config {

import org.springframework.beans.factory.InitializingBean
import java.lang.reflect.Method
import akka.actor.{Actor, ActorSystem}
import akka.spring.config.util.ComponentReflectionUtilities
import org.springframework.util.Assert
import org.springframework.beans.factory.config.BeanPostProcessor


/**
 * @author Josh Long
 */
class ActorBeanPostProcessor extends BeanPostProcessor with InitializingBean {

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  var system:ActorSystem = ActorSystem()

  private[this] def log(msg: String) {
    Console.println(msg);
  }

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = {
    log("postProcessAfterInitialization(bean, '" + beanName + "')")
    bean
  }

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    log("postProcessAfterInitialization " + beanName)
    // is it an @Actor ?
    val isActor = bean.getClass.getAnnotation(classOf[akka.spring.Actor]) != null
    if (isActor) {
      log("creating a DelegatingActor for bean " + bean)
      val a: AR = system.actorOf(new DelegatingActor(bean)) // do NOT sperate the Actor creation from system.actorOf() call.
      return a
    }
    bean
  }

  def afterPropertiesSet() {
    log("afterPropertiesSet()")
  }
}

/**
 * wrapper class for the Actor.
 */
class DelegatingActor(d: AnyRef) extends Actor {

  var receiveMethod: Method = null;
  val delegate = d
  setup(delegate)

  private def setup(b: AnyRef) {
    val receiveAnnotation = classOf[akka.spring.Receive];
    receiveMethod = ComponentReflectionUtilities.findMethodWithAnnotation(b, receiveAnnotation)
    Assert.notNull(receiveMethod,
      "there must be one (and only " +
        "one) method annotated with @Receive")
  }

  protected def doReceive(msg: AnyRef) {
    val ar = msg.asInstanceOf[AnyRef];
    receiveMethod.invoke(delegate, ar)
  }

  protected def receive = {
    case e: AnyRef => doReceive(e)
  }
}

}

package akka.spring.config.util {

import java.lang.reflect.Method
import org.springframework.util.{ReflectionUtils, Assert}
import reflect.BeanProperty


/**
 * Sifting through methods is dirty business. This class handles that chore
 * (and, possibly) others related to inspecting the components at runtime.
 *
 * @author Josh Long
 */
object ComponentReflectionUtilities {

  import java.lang.annotation.Annotation

  def findMethodWithAnnotation(bean: AnyRef, annotation: Class[_ <: Annotation]): Method = {
    val methods = findMethodsWithAnnotation(bean, annotation)
    Assert.isTrue(methods.size == 1, "there should not be more than one match for methods with the annotation " + annotation.getClass.getName)
    methods(0)
  }

  def findMethodsWithAnnotation(bean: AnyRef, annotation: Class[_ <: Annotation]): List[Method] = {
    val methodFilter = new HandlerResolvingMethodFilter(annotation);
    val methodCallback = new HandlerResolvingMethodCallback(annotation);
    ReflectionUtils.doWithMethods(bean.getClass, methodCallback, methodFilter)
    methodCallback.getMethods()
  }

  /**
   * Simply records the method that matches (or throws an exception if the m's already been set)
   */
  private class HandlerResolvingMethodCallback(annotation: Class[_ <: Annotation]) extends ReflectionUtils.MethodCallback {

    @BeanProperty
    var methods: List[Method] = List();

    def doWith(m: Method) {
      this.methods = m :: methods
    }
  }

  /**
   * Preserves any object that has an annotation matching the specified annotation type
   */
  private class HandlerResolvingMethodFilter(annotation: Class[_ <: Annotation]) extends ReflectionUtils.MethodFilter {
    def matches(method: Method) = {
      method.getAnnotation(this.annotation) != null
    }
  }

}

}