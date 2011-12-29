package akka.spring.implementation


import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.String
import org.springframework.util.ReflectionUtils
import org.springframework.util.ReflectionUtils.{MethodFilter, MethodCallback, FieldFilter, FieldCallback}
import org.springframework.beans.BeanUtils
import java.lang.reflect.{AccessibleObject, Method, Field}
import org.springframework.beans.factory.annotation.InjectionMetadata
import org.springframework.beans.factory.support.{MergedBeanDefinitionPostProcessor, RootBeanDefinition}


// provides a default, algorithmic approach to injecting references qualified by an annotation
class ReferenceProvidingPostProcessor [T <: AnyRef, X <: java.lang.annotation.Annotation] (annotation: Class[X], factory: (X) => T) extends BeanPostProcessor {

  private def findActorRefMetadata( beanType : Class[_]) : InjectionMetadata = null

  def postProcessMergedBeanDefinition(beanDefinition: RootBeanDefinition, beanType: Class[_], beanName: String) {
    if (beanType != null) {
      var metadata : InjectionMetadata = findActorRefMetadata(beanType)
      metadata.checkConfigMembers(beanDefinition)
    }
  }

  val fieldFilter = new FieldFilter {
    def matches(f: Field) = f.getAnnotation(annotation) != null
  }

  class SetterMethodFilter(setterObject: AnyRef) extends MethodFilter {
    def matches(m: Method) = m.getAnnotation(annotation) != null && isSetter(setterObject, m)
  }

  /**
   * this should be in some sort of utility class. gross.
   */
  private def doWithElevatedAccessibility[T <: AccessibleObject](t: T, toDo: T => Unit) {
    val oldVisibility = t.isAccessible
    try {
      t.setAccessible(true)
      toDo(t)
    } finally {
      t.setAccessible(oldVisibility)
    }
  }

  class FieldInjectionCallback(fieldObject: AnyRef) extends FieldCallback {
    def doWith(field: Field) {
      val ann = field.getAnnotation(annotation)
      doWithElevatedAccessibility[Field](field, f => f.set(fieldObject, factory(ann)))
    }
  }

  class SetterInjectionCallback(fieldObject: AnyRef) extends MethodCallback {
    def doWith(method: Method) {
      val ann = method.getAnnotation(annotation)
      doWithElevatedAccessibility[Method](method, (m: Method) => m.invoke(fieldObject, factory(ann)))
    }
  }

  def isSetter(obj: AnyRef, method: Method) = BeanUtils.getPropertyDescriptors(obj.getClass).find(p => p.getWriteMethod.equals(method)).isDefined

  def injectSetters(bean: AnyRef) {
    val clazz = bean.getClass
    val methodCallback = new SetterInjectionCallback(bean)
    val methodFilter = new SetterMethodFilter(bean)
    ReflectionUtils.doWithMethods(clazz, methodCallback, methodFilter)
  }

  def injectFields(bean: AnyRef) {
    val clazz = bean.getClass
    val fieldCallback = new FieldInjectionCallback(bean)
    ReflectionUtils.doWithFields(clazz, fieldCallback, fieldFilter)
  }

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = bean

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    injectSetters(bean)
    injectFields(bean)
    bean
  }


}