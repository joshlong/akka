package akka.spring.implementation

import org.springframework.beans.factory.support.{RootBeanDefinition, MergedBeanDefinitionPostProcessor}
import org.springframework.beans.factory.annotation.InjectionMetadata
import org.springframework.beans.factory.BeanCreationException
import java.beans.PropertyDescriptor
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
import org.springframework.beans.PropertyValues
import java.util.LinkedList
import java.lang.{String, Class}
import java.lang.reflect.{Method, Field, Member, Modifier}
import org.apache.commons.logging.LogFactory

/**
 *
 * Generically iterates over bean definitions and injects references to objects based on the presence of a specified annotation.
 * For this class to work, it needs to know which annotation to process (on fields and methods) and it needs to know what to inject (which it does by calling the 
 * provided factory method with the annotation, once encountered).
 *
 * @author Josh Long
 */
class AnnotatedSiteInjectionPostProcessor [T <: AnyRef, X <: java.lang.annotation.Annotation]
  (annotation: Class[X], fieldMetadataCallback: (X, Field) => InjectionMetadata.InjectedElement, methodMetadataCallback: (X, Method) => InjectionMetadata.InjectedElement)
  extends InstantiationAwareBeanPostProcessor with MergedBeanDefinitionPostProcessor {
  
  
  val logger = LogFactory.getLog(getClass)

  def postProcessBeforeInstantiation(beanClass: Class[_], beanName: String) = null

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String) = bean

  def postProcessAfterInstantiation(bean: AnyRef, beanName: String) = true

  def postProcessAfterInitialization(bean: AnyRef, beanName: String) = bean

  def postProcessMergedBeanDefinition(beanDefinition: RootBeanDefinition, beanType: Class[_], beanName: String) {
    if (beanType != null) {
      var metadata: InjectionMetadata = findInjectionSiteMetadata(beanType)
      metadata.checkConfigMembers(beanDefinition)
    }
  }

  def postProcessPropertyValues(pvs: PropertyValues, pds: Array[PropertyDescriptor], bean: AnyRef, beanName: String): PropertyValues = {
    try {
     // var s = ""
     // pds.foreach( st => s += st.getWriteMethod.toString )
      logger.info("about to attempt injection for class "+ bean.getClass )
      val metadata = findInjectionSiteMetadata(bean.getClass)
      metadata.inject(bean, beanName, pvs)
    } catch {
      case ex: Throwable =>
        throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex)
    }
    pvs
  }

  private def doWithMembers[T <: Member](clazz: Class[_], filter: T => Boolean, fieldsFactory: (Class[_]) => Array[T], doWith: T => Unit) = {
    var targetClass: Class[_] = clazz
    do {
      fieldsFactory(targetClass).filter(filter).foreach(doWith)
      targetClass = targetClass.getSuperclass
    } while (targetClass != null && !targetClass.equals(classOf[AnyRef]))
  }

  private def findInjectionSiteMetadata(clazz: Class[_]): InjectionMetadata = {

    val currElements = new LinkedList[InjectionMetadata.InjectedElement]

    doWithMembers[Field](clazz, f => !Modifier.isStatic(f.getModifiers) && f.getAnnotation(annotation) != null, c => c.getDeclaredFields, f => {
      val fieldAnnotation = f.getAnnotation(annotation)
      val injectedElement = fieldMetadataCallback(fieldAnnotation, f)
      currElements.add(injectedElement)
    })

    doWithMembers[Method](clazz, f => !Modifier.isStatic(f.getModifiers) && f.getAnnotation(annotation) != null, c => c.getDeclaredMethods, m => {
      val methodAnnotation = m.getAnnotation(annotation)
      val injectedElement = methodMetadataCallback(methodAnnotation, m)
      currElements.add(injectedElement)
    })

    new InjectionMetadata(clazz, currElements)

  }


}