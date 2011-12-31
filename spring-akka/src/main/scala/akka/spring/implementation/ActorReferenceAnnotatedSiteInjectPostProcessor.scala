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
    (ar: ActorReference, f: Field) => new ActorReferenceInjectedElement(actorSystem, ar, f, null),
    (ar: ActorReference, m: Method) => new ActorReferenceInjectedElement(actorSystem, ar, m, BeanUtils.findPropertyForMethod(m)))


class ActorReferenceInjectedElement(actorSystem: ActorSystem, annotation:ActorReference, member: Member, propertyDescriptor: PropertyDescriptor) extends InjectionMetadata.InjectedElement(member, propertyDescriptor) {
  override def getResourceToInject(target: AnyRef, requestingBeanName: String) = actorSystem.actorFor(annotation.value() )
}



