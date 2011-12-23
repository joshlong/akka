package akka.spring.config {

import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import org.springframework.beans.factory.config.BeanPostProcessor
import akka.japi.Creator
import reflect.BeanProperty
import akka.actor._
import akka.spring.config.util.{Argument, HandlerMetadata, ComponentReflectionUtilities}
import collection.mutable.HashMap


/**
 * @author Josh Long
 */
class ActorBeanPostProcessor extends BeanPostProcessor with InitializingBean {

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  // todo this should be injected and instantiated by a factory bean

  @BeanProperty var system: ActorSystem = ActorSystem()

  @BeanProperty var props: Props = Props() // this is overrideable  use defaults for now

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
      val props = this.props.withCreator(new Creator[Actor] {
        def create(): Actor = new DelegatingActor(bean)
      });
      return system.actorOf(props);
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

  private def log(s: String) {
    Console.println(s);
  }

  protected def doReceive(msg: AnyRef) {

    val c: ActorContext = this.context
    val s: ActorRef = this.self


    log("inside the doReceive method about to invoke a handler method " + this.handlers.length);

    // lets find the right handler method that accepts a payload of the same type as specified here

    this.handlers.foreach((handler: HandlerMetadata) => {

      val mapOfArgs = new HashMap[Int, AnyRef]

      handler.payload.get match {
        case null => throw new RuntimeException("You must have a paramter that accepts the payload of the message! ")
        case ar: Argument => {
          // build a map that takes integer arg poisitions as keys
          // then find the highest integer key, N
          // then iterate through, 0->N, and add the values for those keys to an Array
          // then use the array to invoke the handler receive method
          Console.println("handler's payload class: " + ar.argumentType.getName + "; actual message class: " + msg.getClass.getName)
          if ((ar.argumentType).isAssignableFrom(msg.getClass)) {
            mapOfArgs += ar.argumentPosition -> msg;

            handler.selfReference.getOrElse(null) match {
              case null => null
              case selfArg: Argument => {
                mapOfArgs += selfArg.argumentPosition -> s
              }

            };

            handler.actorContextReference.getOrElse(null) match {
              case null => null
              case acRef: Argument => {
                mapOfArgs += acRef.argumentPosition -> c
              }
            }

            var args = List[AnyRef]()
            // now, iterate through the map and then build an array and actually invoke the method 
            mapOfArgs.keySet.toList.sorted.foreach((k: Int) => {
              args = mapOfArgs(k) :: args
            })

            val argsForInvocation = args.reverse;
            log(argsForInvocation.toString())
            // todo fix me

            handler.method.invoke(this.delegate, argsForInvocation: _*) // the '_*' tells scala that we want to use this sequence as a varargs expansion
          }
        }
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
import akka.spring.Receive

/**
 * stores information about a single argument to a handler method 
 */
class Argument(@BeanProperty var argumentType: Class[_], @BeanProperty var argumentPosition: Int, @BeanProperty var argumentAnnotation: Annotation)

//(var argumentType: Class[_], var argumentPosition: Int, var argumentAnnoation: Annotation)


/**
 * Wraps the metadata we need t
 */
class HandlerMetadata {
  @BeanProperty var method: Method = _
  @BeanProperty var selfReference: Option[Argument] = None
  @BeanProperty var actorContextReference: Option[Argument] = None
  @BeanProperty var payload: Option[Argument] = None
}


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

  private def log(s: String) {
    Console.println(s);
  }

  private class HandlerResolvingMethodCallback(annotation: Class[_ <: Annotation]) extends ReflectionUtils.MethodCallback {


    @BeanProperty
    var metadata: List[HandlerMetadata] = List();

    /**
     * the idea is that we need a clean way to handle different types of paramters in the processing pipeline 
     * hide that logic here and let the inspection routine live independantly of the annotation processing logic.
     */
    private def doWithParameterType[T <: Annotation](m: Method, metadata: HandlerMetadata, annotationType: Class[T], callback: (Int, Class[_], HandlerMetadata, T) => Unit) = {
      val paramTypes: Array[Class[_]] = m.getParameterTypes
      val paramAnnotations: Array[Array[Annotation]] = m.getParameterAnnotations
      var ctr: Int = 0

      paramTypes.foreach((c: Class[_]) => {

        log("we have found parameter type : " + c.getName + "; the annotation type were looking for is " + annotationType.getName)
        val annotationsForParam: Array[Annotation] = paramAnnotations(ctr)
        val matched: Option[Annotation] = annotationsForParam.find((a: Annotation) => {
          annotationType.isAssignableFrom(a.getClass)
        })
        matched.getOrElse(null) match {
          case null => null
          case a: T => {
            log("found a paramter with an annotation of type " + annotationType.getName)
            callback(ctr, c, metadata, a)
          }
        }
        ctr += 1
      });


    }

    def doWith(m: Method) {
      if (m.getAnnotation(classOf[Receive]) != null) {

        val hm = new HandlerMetadata()

        hm.method = m

        doWithParameterType(m, hm, classOf[akka.spring.Payload], (argPosition: Int, argType: Class[_], handlerMetadData: HandlerMetadata, annotationForParam: akka.spring.Payload) => {
          hm.payload = Some(new Argument(argType, argPosition, annotationForParam))
        });

        doWithParameterType(m, hm, classOf[akka.spring.Self], (argPos: Int, argType: Class[_], metadata: HandlerMetadata, annotationForParameter: akka.spring.Self) => {
          hm.selfReference = Some(new Argument(argType, argPos, annotationForParameter))
        })

        this.metadata = hm :: this.metadata;

      }


    }
  }


}

}