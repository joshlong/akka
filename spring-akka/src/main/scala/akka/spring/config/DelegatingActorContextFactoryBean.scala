package akka.spring.config

import akka.actor.ActorContext
import org.springframework.beans.factory.{BeanFactory, BeanFactoryAware, FactoryBean}
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.util.ClassUtils
import org.aopalliance.intercept.{MethodInvocation, MethodInterceptor}

class DelegatingActorContextFactoryBean extends FactoryBean[ActorContext] with BeanFactoryAware {

  var cachedBeanFactory: BeanFactory = _

  def getObject: ActorContext = {
    val interfacesToProxy: Array[Class[_]] = Array(classOf[ActorContext])
    val proxyFactoryBean = new ProxyFactoryBean
    proxyFactoryBean.setProxyClassLoader(ClassUtils.getDefaultClassLoader)
    proxyFactoryBean.setProxyInterfaces(interfacesToProxy)
    proxyFactoryBean.addAdvice(new ActorLocalDelegatingActorContextHandler)
    proxyFactoryBean.setBeanFactory(this.cachedBeanFactory)
    proxyFactoryBean.getObject.asInstanceOf[ActorContext]
  }

  def getObjectType = classOf[ActorContext]

  def isSingleton = true

  def setBeanFactory(beanFactory: BeanFactory) {
    this.cachedBeanFactory = beanFactory
  }

  private[this] class ActorLocalDelegatingActorContextHandler extends MethodInterceptor {
    def invoke(invocation: MethodInvocation): AnyRef = {
      var actorContext = ActorLocalStorage.current.get().context
      val args = invocation.getArguments
      invocation.getMethod.invoke(actorContext, args : _* )
    }
  }

}