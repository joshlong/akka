package akka.spring.config {

import org.springframework.beans.factory.InitializingBean
import akka.actor.{Actor, ActorSystem}
import org.springframework.util.Assert
import org.springframework.beans.factory.config.BeanPostProcessor
import akka.spring.config.util.{HandlerMetadata, ComponentReflectionUtilities}
import collection.mutable.HashMap


/**
 * @author Josh Long
 */
class ActorBeanPostProcessor extends BeanPostProcessor with InitializingBean {

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  var system: ActorSystem = ActorSystem()

  private[this] def log(msg: String) {
    Console.println(msg);
  }

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = {
    log("postProcessAfterInitialization(bean, '" + beanName + "')")
    bean
  }

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    log("postProcessBeforeInitialization " + beanName)
    // is it an @Actor ?
    val isActor = bean.getClass.getAnnotation(classOf[akka.spring.Actor]) != null
    if (isActor) {
      log("creating a DelegatingActor for bean " + bean)
      return system.actorOf(new DelegatingActor(bean)) // do NOT sperate the Actor creation from system.actorOf() call.
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
class DelegatingActor(delegate: AnyRef) extends Actor {

  val receiveAnnotation = classOf[akka.spring.Receive]
  var handlers: List[HandlerMetadata] = _
  setup(delegate);

  private def setup(b: AnyRef) {
    handlers = ComponentReflectionUtilities.findMethodsWithAnnotation(b, receiveAnnotation)
    Assert.notNull(handlers.size > 0, "there must be at least one method annotated with " + receiveAnnotation.getClass.getName)
  }

  protected def doReceive(msg: AnyRef) {
    Console.println("inside the doReceive method about to invoke a handler method");
    // lets find the right handler method that accepts a payload of the same type as specified here
    this.handlers.foreach((handler: HandlerMetadata) => {
      val payloadArg = handler.payload
      val pClazz = payloadArg.argumentType;
      val mClazz = msg.getClass
      val m = handler.method
      if (pClazz.equals(mClazz) || pClazz.isAssignableFrom(mClazz)) {

      }
    });


  }

  protected def receive = {
    case e: AnyRef => doReceive(e)
  }
}

}

package akka.spring.config.util {

import java.lang.reflect.Method
import org.springframework.util.ReflectionUtils
import reflect.BeanProperty
import java.lang.annotation.Annotation

/**
 * stores information about a single argument to a handler method 
 */
class Argument(var argumentType: Class[_], var argumentPosition: Int, var argumentAnnoations: Array[_ <: Annotation])


/**
 * Wraps the metadata we need t
 */
class HandlerMetadata(var method: Method, var payload: Argument)

/**
 * Sifting through methods is dirty business. This class handles that chore
 * (and, possibly) others related to inspecting the components at runtime.
 *
 * @author Josh Long
 */
object ComponentReflectionUtilities {

  def findMethodsWithAnnotation(bean: AnyRef, annotation: Class[_ <: Annotation]): List[HandlerMetadata] = {
    val methodCallback = new HandlerResolvingMethodCallback(annotation);
    ReflectionUtils.doWithMethods(bean.getClass, methodCallback)
    methodCallback.getMetadata()
  }

  private class HandlerResolvingMethodCallback(annotation: Class[_ <: Annotation]) extends ReflectionUtils.MethodCallback {

    @BeanProperty
    var metadata: List[HandlerMetadata] = List();

    def doWith(m: Method) {
      // id this a handler method? does it specify the @Receive annotation? 
      if (m.getAnnotation(this.annotation) != null) {
        ///then iterate through the arguments 
        val paramTypes: Array[Class[_]] = m.getParameterTypes
        val paramAnnotations: Array[Array[Annotation]] = m.getParameterAnnotations
        var ctr: Int = 0

        paramTypes.foreach((c: Class[_]) => {

          // @Payload  - is this the payload argument parameter 
          val annotationsForParam = paramAnnotations(ctr)
          if (annotationsForParam.indexOf(classOf[akka.spring.Payload]) >= 0) {
            val payloadArgument = new Argument(c, ctr, annotationsForParam)
            metadata = new HandlerMetadata(m, payloadArgument) :: metadata
          }

          ctr += 1

        });


      }
    }
  }


}

}