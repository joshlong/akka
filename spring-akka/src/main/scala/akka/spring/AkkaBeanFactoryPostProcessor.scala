package akka.spring

import akka.spring.config.util.Log._
import implementation.{ActorBeanPostProcessor, ActorSystemFactoryBean, DelegatingActorContextFactoryBean}
import org.springframework.beans.factory.config.{ConfigurableListableBeanFactory, BeanFactoryPostProcessor}
import org.springframework.beans.factory.support.{GenericBeanDefinition, BeanDefinitionRegistry}

class AkkaBeanFactoryPostProcessor extends BeanFactoryPostProcessor {

  def registerBeanIfItDoesntExist[T](bdf: BeanDefinitionRegistry, clazz: Class[T], callback: (String, GenericBeanDefinition) => Unit) {
    val namingFunction = (n: Class[_]) => {
      n.getName.toLowerCase
    }
    bdf.getBeanDefinitionNames.find((beanDefinitionName: String) => {
      val beanDef = bdf.getBeanDefinition(beanDefinitionName)
      val beanClassName = beanDef.getBeanClassName
      beanClassName != null && beanClassName.equals(clazz.toString)
    }).isDefined match {
      case false => {
        val beanDef = new GenericBeanDefinition()
        beanDef.setBeanClass(clazz)
        callback(namingFunction(clazz), beanDef)
      }
    }

  }

  protected def registerBeans(bdf: BeanDefinitionRegistry) {

    // then register the thread safe ActorContext
    registerBeanIfItDoesntExist(bdf, classOf[DelegatingActorContextFactoryBean], (name: String, beanDefinition: GenericBeanDefinition) => {
      bdf.registerBeanDefinition(name, beanDefinition)
    });

    // then register the ActorSystem
    registerBeanIfItDoesntExist(bdf, classOf[ActorSystemFactoryBean], (name: String, beanDef: GenericBeanDefinition) => {
      bdf.registerBeanDefinition(name, beanDef)
    })

    // first register the BPP
    registerBeanIfItDoesntExist(bdf, classOf[ActorBeanPostProcessor], (name: String, beanDefinition: GenericBeanDefinition) => {
      bdf.registerBeanDefinition(name, beanDefinition)
    })
  }

  def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    log("inside the " + getClass.getName)
    if (beanFactory.isInstanceOf[BeanDefinitionRegistry]) {
      val registry: BeanDefinitionRegistry = beanFactory.asInstanceOf[BeanDefinitionRegistry]
      registerBeans(registry)
    }

  }
}