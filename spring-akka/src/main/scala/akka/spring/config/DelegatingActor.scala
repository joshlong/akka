package akka.spring.config

import akka.actor.{ActorRef, ActorContext, Actor}
import util.Log._
import collection.mutable.HashMap
import akka.spring.config.util.{Argument, HandlerMetadata}
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import java.lang.annotation.Annotation


class DelegatingActor(delegate: AnyRef) extends Actor {

  val handlers: List[HandlerMetadata] = {
    val methodCallback = new HandlerResolvingMethodCallback(classOf[akka.spring.Receive]);
    ReflectionUtils.doWithMethods(delegate.getClass, methodCallback)
    methodCallback.metadata
  }

  protected def doReceive(msg: AnyRef) {
    val c: ActorContext = this.context
    val s: ActorRef = this.self
    log("inside the doReceive method about to invoke a handler method " + this.handlers.length);

    this.handlers.foreach((handler: HandlerMetadata) => {

      val mapOfArgs = new HashMap[Int, AnyRef]

      handler.payload.get match {
        case null => throw new RuntimeException("You must have a parameter that accepts the payload of the message! ")
        case ar: Argument => {
          // build a map that takes integer arg poisitions as keys
          // then find the highest integer key, N
          // then iterate through, 0->N, and add the values for those keys to an Array
          // then use the array to invoke the handler receive method

          log("handler's payload class: " + ar.argumentType.getName + "; actual message class: " + msg.getClass.getName)

          if ((ar.argumentType).isAssignableFrom(msg.getClass)) {
            // then this is the handler method we should call!
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

            try {

              ActorLocalStorage.current.get() match {
                case null => ActorLocalStorage.current.set(new ActorLocalStorage(s, c))
              }
              handler.method.invoke(this.delegate, argsForInvocation: _*) // the '_*' tells scala that we want to use this sequence as a varargs expansion
            } finally {
              ActorLocalStorage.current.remove()
            }
          }
        }
      }


    });
  }


  protected def receive = {
    case e: AnyRef => doReceive(e)
  }
}

private class HandlerResolvingMethodCallback(annotation: Class[_ <: Annotation]) extends ReflectionUtils.MethodCallback {

  var _metadata: List[HandlerMetadata] = List();

  def metadata = this._metadata

  private def doWithParameterType[T <: Annotation](m: Method, me: HandlerMetadata, annotationType: Class[T], callback: (Int, Class[_], HandlerMetadata, T) => Unit) = {
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
          callback(ctr, c, me, a)
        }
      }
      ctr += 1
    });
  }

  def doWith(m: Method) {
    if (m.getAnnotation(classOf[akka.spring.Receive]) != null) {

      val hm = new HandlerMetadata()
      hm.method = m

      doWithParameterType(m, hm, classOf[akka.spring.Payload], (argPosition: Int, argType: Class[_], handlerMetadData: HandlerMetadata, annotationForParam: akka.spring.Payload) => {
        hm.payload = Some(new Argument(argType, argPosition, annotationForParam))
      });

      doWithParameterType(m, hm, classOf[akka.spring.Self], (argPos: Int, argType: Class[_], metadata: HandlerMetadata, annotationForParameter: akka.spring.Self) => {
        hm.selfReference = Some(new Argument(argType, argPos, annotationForParameter))
      })

      this._metadata = hm :: this._metadata;
    }
  }

}
