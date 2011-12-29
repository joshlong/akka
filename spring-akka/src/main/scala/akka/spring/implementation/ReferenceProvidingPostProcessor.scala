package akka.spring.implementation


import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.String
import org.springframework.util.ReflectionUtils
import java.lang.reflect.{Method, Field}
import org.springframework.util.ReflectionUtils.{MethodFilter, MethodCallback, FieldFilter, FieldCallback}
import org.springframework.beans.BeanUtils


// provides a default, algorithmic approach to injecting references qualified by an annotation
class ReferenceProvidingPostProcessor[T <: AnyRef](annotation: Class[_ <: java.lang.annotation.Annotation], factory: () => T) extends BeanPostProcessor {

  val fieldFilter = new FieldFilter {
    def matches(f: Field) = f.getAnnotation(annotation) != null
  }

  class FieldInjectionCallback(fieldObject: AnyRef, ref: AnyRef) extends FieldCallback {
    def doWith(field: Field) {
      field.set(fieldObject, ref)
    }
  }

  class SetterMethodFilter(setterObject: AnyRef) extends MethodFilter {
    def matches(m: Method) = m.getAnnotation(annotation) != null && isSetter(setterObject, m)
  }

  class SetterInjectionCallback(fieldObject: AnyRef, ref: AnyRef) extends MethodCallback {
    def doWith(method: Method) {
      method.invoke(fieldObject, ref)
    }
  }

  def isSetter(obj: AnyRef, method: Method) = BeanUtils.getPropertyDescriptors(obj.getClass).find(p => p.getWriteMethod.equals(method)).isDefined

  def injectSetters(bean: AnyRef, ref: AnyRef) {
    val clazz = bean.getClass
    val methodCallback = new SetterInjectionCallback(bean, ref)
    val methodFilter = new SetterMethodFilter(bean)
    ReflectionUtils.doWithMethods(clazz, methodCallback, methodFilter)
  }

  def injectFields(bean: AnyRef, ref: AnyRef) {
    val clazz = bean.getClass
    val fieldCallback = new FieldInjectionCallback(bean, ref)
    ReflectionUtils.doWithFields(clazz, fieldCallback, fieldFilter)
  }

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = bean

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    injectSetters(bean, factory())
    injectFields(bean, factory())
    bean
  }


}