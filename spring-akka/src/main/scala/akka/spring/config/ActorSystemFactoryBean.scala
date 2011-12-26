package akka.spring.config


import akka.spring.config.util.Log.log

import akka.japi.Creator
import reflect.BeanProperty
import akka.actor._
import collection.mutable.HashMap
import org.aopalliance.intercept.{MethodInvocation, MethodInterceptor}
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.util.{ClassUtils, Assert}
import org.springframework.beans.factory.{BeanFactory, BeanFactoryAware, FactoryBean, InitializingBean}
import org.springframework.beans.factory.config.BeanPostProcessor
import akka.spring.config.util.{Argument, HandlerMetadata, ComponentReflectionUtilities}


// todo make this more impressive, and make it support all the options that the config file format supports.
class ActorSystemFactoryBean extends FactoryBean[ActorSystem] {

  def isSingleton = true

  def getObjectType = classOf[ActorSystem]

  def getObject = ActorSystem()
}