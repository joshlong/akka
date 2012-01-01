package akka.spring.mailbox

import akka.dispatch._
import akka.actor.{ActorCell, ActorContext, ActorRef}
import com.typesafe.config.Config

/**
 * Configure the Mailbox you'd like to use in Spring and then set it on the [[akka.spring.ActorSystemFactoryBean]].
 *
 * @author Josh Long
 */
object DelegatingUnmanagedMailbox {
  var mailbox: Mailbox = _
}

class DelegatingUnmanagedMailboxType(config: Config) extends MailboxType {

  def create(receiver: ActorContext) =
      DelegatingUnmanagedMailbox.mailbox match {
        case null => throw new RuntimeException("the " + classOf[DelegatingUnmanagedMailbox] + ".mailbox should not be null!")
        case _ => new DelegatingUnmanagedMailbox(receiver, DelegatingUnmanagedMailbox.mailbox)
      }


}

class DelegatingUnmanagedMailbox(owner: ActorContext, delegate: Mailbox) extends Mailbox(owner.asInstanceOf[ActorCell]) {

  def enqueue(receiver: ActorRef, handle: Envelope) {
    delegate.enqueue(receiver, handle)
  }

  def dequeue() = delegate.dequeue()

  def numberOfMessages = delegate.numberOfMessages

  def hasMessages = delegate.hasMessages

  def systemEnqueue(receiver: ActorRef, message: SystemMessage) {
    delegate.systemEnqueue(receiver, message)
  }

  def systemDrain() = delegate.systemDrain()

  def hasSystemMessages = delegate.hasSystemMessages
}
