package akka.spring.config.util

import org.springframework.util.ReflectionUtils
import reflect.BeanProperty
import java.lang.reflect.Method
import akka.spring.config.util.Log._
import akka.spring.Receive
import java.lang.annotation.Annotation


object ComponentReflectionUtilities {

  def findMethodsWithAnnotation(bean: AnyRef, annotation: Class[_ <: Annotation]): List[HandlerMetadata] = {
    val methodCallback = new HandlerResolvingMethodCallback(annotation);
    ReflectionUtils.doWithMethods(bean.getClass, methodCallback)
    methodCallback.getMetadata()
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