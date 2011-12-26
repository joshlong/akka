package akka.spring {

import akka.spring.config.util.Log._
import config.{ActorSystemFactoryBean, DelegatingActorContextFactoryBean, ActorBeanPostProcessor}
import org.springframework.beans.factory.config.{ConfigurableListableBeanFactory, BeanFactoryPostProcessor}
import org.springframework.beans.factory.support.{GenericBeanDefinition, BeanDefinitionRegistry}

/**
 *
 * This BFPP installs all the objects that you need to start using Spring with Akka
 *
 * @author Josh Long 
 */
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

}

package akka.spring.config {

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

/**
 *
 * A simple Spring BeanPostProcessor that finds all beans that have the Actor stereotype annotation
 * and inspects them for particular handler methods.
 *
 * TOOD it would be ideal if this could be built on top of the typed actor implementation.
 *
 */
class ActorBeanPostProcessor extends BeanPostProcessor with InitializingBean {

  type A = akka.actor.Actor
  type AR = akka.actor.ActorRef

  //todo replace this with the ActorSystem from the ActorSystemFactoryBean
  @BeanProperty var system: ActorSystem = ActorSystem()

  @BeanProperty var props: Props = Props()

  def postProcessAfterInitialization(bean: AnyRef, beanName: String): AnyRef = {
    bean
  }

  def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    val isActor = bean.getClass.getAnnotation(classOf[akka.spring.Actor]) != null

    if (isActor) {
      val createdActor = this.props.withCreator(new Creator[Actor] {
        def create(): Actor = new DelegatingActor(bean)
      });
      return system.actorOf(createdActor);
    }

    bean
  }

  def afterPropertiesSet() {
    log("afterPropertiesSet()")
  }
}

/**
 * Holds the state for the current actor during an invocation 
 */
class ActorLocalStorage(var self: ActorRef, var context: ActorContext)

object ActorLocalStorage {
  val current = new ThreadLocal[ActorLocalStorage]();
}

/**
 * wrapper class for the Actor.
 */
class DelegatingActor(delegate: AnyRef) extends Actor {

  val receiveAnnotation = classOf[akka.spring.Receive]
  var handlers: List[HandlerMetadata] = _
  setup(delegate);

  private def setup(b: AnyRef) {
    handlers = ComponentReflectionUtilities.findMethodsWithAnnotation(b, receiveAnnotation)
    Assert.notNull(handlers.size > 0, "there must be at least one method annotated with " + receiveAnnotation.getClass.getName)
  }

  protected def doReceive(msg: AnyRef) {

    val c: ActorContext = this.context
    val s: ActorRef = this.self

    log("inside the doReceive method about to invoke a handler method " + this.handlers.length);

    // lets find the right handler method that accepts a payload of the same type as specified here

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


/**
 * installs a ActorContext that is thread safe, delegating to the ActorContext associated with each Actor 
 */
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


/**
 * the idea is that this class will handle all the chores typically assumed of an ActorContext, except that
 * it will delegate to a thread bound  ActorConitext, which means that users can injects this 
 * bean and call it without fear of it being correctly bound.
 */


}

package akka.spring.config.util {

import java.lang.reflect.Method
import org.springframework.util.ReflectionUtils
import reflect.BeanProperty
import java.lang.annotation.Annotation
import akka.spring.Receive
import akka.spring.config.util.Log.log
import org.apache.log4j.Logger

/**
 * stores information about a single argument to a handler method 
 */
class Argument(@BeanProperty var argumentType: Class[_], @BeanProperty var argumentPosition: Int, @BeanProperty var argumentAnnotation: Annotation)

//(var argumentType: Class[_], var argumentPosition: Int, var argumentAnnoation: Annotation)


/**
 * Wraps the metadata we need t
 */
class HandlerMetadata {
  @BeanProperty var method: Method = _
  @BeanProperty var selfReference: Option[Argument] = None
  @BeanProperty var actorContextReference: Option[Argument] = None
  @BeanProperty var payload: Option[Argument] = None
}


object Log {
  def log(a: String) {
    Logger.getLogger(getClass).info(a)
  }
}


/**
 * Sifting through methods is dirty business. This class handles that chore
 * (and, possibly) others related to inspecting the components at runtime.
 *
 * @author Josh Long
 */
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

}