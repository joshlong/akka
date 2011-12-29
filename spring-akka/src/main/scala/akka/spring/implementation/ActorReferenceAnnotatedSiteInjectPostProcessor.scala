package akka.spring.implementation

import java.beans.PropertyDescriptor
import org.springframework.beans.factory.annotation.InjectionMetadata
import akka.spring.ActorReference
import java.lang.reflect.{Method, Field, Member}
import org.springframework.beans.BeanUtils
import akka.actor.{ActorRef, ActorSystem}

/**
 * the [[akka.spring.ActorReference]] annotation marks properties and fields that designate sites where references to [[akka.actor.ActorRef]]s should be injected.
 *
 * @author Josh Long
 * @param actorSystem the [[akka.actor.ActorSystem]] that should be used to consult and lookup [[akka.actor.ActorRef]] references
 */
class ActorReferenceAnnotatedSiteInjectPostProcessor(actorSystem: ActorSystem)
  extends AnnotatedSiteInjectionPostProcessor[ActorRef, ActorReference](
    classOf[akka.spring.ActorReference],
    (ar: ActorReference, f: Field) => new ActorReferenceInjectedElement(actorSystem, f, null),
    (ar: ActorReference, m: Method) => new ActorReferenceInjectedElement(actorSystem, m, BeanUtils.findPropertyForMethod(m)))


private class ActorReferenceInjectedElement(as: ActorSystem, member: Member, pd: PropertyDescriptor) extends InjectionMetadata.InjectedElement(member, pd) {

  private val actorPath: String = {

    val a: ActorReference = {
      if (member.getClass.isAssignableFrom(classOf[Field])) {
        val f = member.asInstanceOf[Field]
        f.getAnnotation(classOf[ActorReference])
      }
      else if (member.getClass.isAssignableFrom(classOf[Method])) {
        val m = member.asInstanceOf[Method]
        m.getAnnotation(classOf[ActorReference])
      }
      else
        throw new RuntimeException("We couldn't retreive the @ActorReference annotation from the class member " + member.toString)
    }
    a.value()
  }

  override def getResourceToInject(target: AnyRef, requestingBeanName: String) = {
    as.actorFor(this.actorPath)
  }
}


/*
 //  Provides a reference to a [[akka.actor.ActorSystem]] for all sites where [[akka.spring.ActorRef]] is found.
class ActorReferenceAnnotationReferenceProvider(as: ActorSystem) extends ReferenceProvidingPostProcessor[akka.actor.ActorRef, akka.spring.ActorRef](classOf[akka.spring.ActorRef], (arAnnotationReference: akka.spring.ActorRef) => {
  val selectorString = arAnnotationReference.value()
  as.actorFor(selectorString)
})
*/