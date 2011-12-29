package akka.spring

import implementation._
import org.springframework.beans.factory.support.{BeanDefinitionBuilder, BeanDefinitionRegistry}
import java.lang.String
import org.springframework.beans.factory.config.{ConfigurableListableBeanFactory, BeanFactoryPostProcessor}
import akka.actor.ActorSystem

/// todo support @ActorRef( selectorString)
/// todo support Futures and so on

class AkkaBeanFactoryPostProcessor extends BeanFactoryPostProcessor {

  private def registerBeanIfItDoesNotExist[T](beanDefinitionRegistry: BeanDefinitionRegistry, clazz: Class[T], callback: (String, BeanDefinitionBuilder) => Unit): String = {

    var currentBeanName: String = null
    var beanName: String = null
    val namingFunction = (clazzToGenerateANameFor: Class[_]) => clazzToGenerateANameFor.getName.toLowerCase

    beanDefinitionRegistry.getBeanDefinitionNames.find((beanDefinitionName: String) => {
      currentBeanName = beanDefinitionName
      val beanDef = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName)
      val beanClassName = beanDef.getBeanClassName
      beanClassName != null && beanClassName.equals(clazz.toString)
    }).isDefined match {
      case true => beanName = currentBeanName
      case false => {
        beanName = namingFunction(clazz)
        val bdf = BeanDefinitionBuilder.genericBeanDefinition(clazz)
        callback(beanName, bdf)
      }
    }
    beanName
  }

  private def registerBeans(beanDefinitionRegistry: BeanDefinitionRegistry) {

    // register the ActorSystem
    val as = registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorSystemFactoryBean], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorReferenceAnnotationReferenceProvider], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinition.addConstructorArgReference(as)
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    // register the thread safe ActorContext
    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[DelegatingActorContextFactoryBean], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    // register the component model mechanism itself
    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorBeanPostProcessor], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinition.addConstructorArgReference(as)
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })
  }

  def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    if (beanFactory.isInstanceOf[BeanDefinitionRegistry]) {
      val registry: BeanDefinitionRegistry = beanFactory.asInstanceOf[BeanDefinitionRegistry]
      registerBeans(registry)
    }
  }

}

/**
 * Provides a reference to a [[akka.actor.ActorSystem]] for all sites where [[akka.spring.ActorRef]] is found.
 */
class ActorReferenceAnnotationReferenceProvider(as: ActorSystem) extends ReferenceProvidingPostProcessor[akka.actor.ActorRef, akka.spring.ActorRef](classOf[akka.spring.ActorRef], (arAnnotationReference: akka.spring.ActorRef) => {
  val selectorString = arAnnotationReference.value()
  as.actorFor(selectorString)
})
