package akka.spring

import _root_..
import implementation._
import org.springframework.beans.factory.config.{ConfigurableListableBeanFactory, BeanFactoryPostProcessor}
import org.springframework.beans.factory.support.{BeanDefinitionBuilder, BeanDefinitionRegistry}
import java.lang.{Class, String}


// todo support Futures and so on

/**
 *
 * This class installs all the support for working with Akka from the Spring component model, including support
 * for injection of other Actors with the [[akka.spring.ActorReference]] annotation,
 * the creation of Akka Actors using the [[akka.spring.Actor]] annotation and the designation of 
 * Actor handler methods with the [[akka.spring.Receive]] annotation.
 *
 * This [[org.springframework.beans.factory.config.BeanFactoryPostProcessor]] does not install the Akka
 * transaction manager, an implementation of [[org.springframework.transaction.PlatformTransactionManager]], but
 * you can easily by simply instantiating it and decorating your [[akka.actor.Actor]] actor handler methods with
 * [[org.springframework.transaction.annotation.Transactional]].
 * 
 * @author Josh Long 
 */
class AkkaBeanFactoryPostProcessor extends BeanFactoryPostProcessor {

  def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    if (beanFactory.isInstanceOf[BeanDefinitionRegistry]) {
      val registry: BeanDefinitionRegistry = beanFactory.asInstanceOf[BeanDefinitionRegistry]
      registerBeans(registry)
    }
  }

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
    val actorSystemBeanName = registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorSystemFactoryBean], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    // register the @ActorReference
    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorReferenceAnnotatedSiteInjectPostProcessor], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinition.addConstructorArgReference(actorSystemBeanName)
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    // register the thread safe ActorContext
    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[DelegatingActorContextFactoryBean], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })

    // register the component model mechanism itself (@Actor, and @Receive)
    registerBeanIfItDoesNotExist(beanDefinitionRegistry, classOf[ActorBeanPostProcessor], (name: String, beanDefinition: BeanDefinitionBuilder) => {
      beanDefinition.addConstructorArgReference(actorSystemBeanName)
      beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition.getBeanDefinition)
    })
  }

}
