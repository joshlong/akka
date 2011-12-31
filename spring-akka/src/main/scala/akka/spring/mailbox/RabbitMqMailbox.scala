package akka.spring.mailbox

import akka.actor.{ActorContext, ActorRef}
import akka.dispatch._
import org.springframework.amqp.rabbit.connection._
import org.springframework.beans.factory.InitializingBean
import org.springframework.amqp.rabbit.core.RabbitTemplate
import scala.None
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate}
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}


/**
 * Simple Akka Mailbox that delegates to RabbitMQ. There should be little reason that generic AMQP couldn't be used instead.
 *
 * Patterned after https://github.com/jboner/akka/blob/master/akka-durable-mailboxes/akka-redis-mailbox/src/main/scala/akka/actor/mailbox/RedisBasedMailbox.scala 
 *
 * @author Josh Long
 */
class RabbitMqMailbox(val owner: ActorContext,
                      val routingKey: String, // to sed messages
                      val exchange: String, // to send messages
                      val queue: String, // from which to receive
                      val rabbitConnection: ConnectionFactory, val transactionManager: Option[PlatformTransactionManager] = None)
  extends CustomMailbox(owner) with MessageQueue with InitializingBean {

  protected def serialise(msg: AnyRef) = msg // todo  

  private val rabbitTemplate = {
    /*
    if (rt.getMessageConverter == null ){
      // todo peerhaps we can register a specific Akka Envelope message converter?
    }
    * */
    val rt = new RabbitTemplate(this.rabbitConnection)
    rt.afterPropertiesSet()
    rt
  }

  private val transactionTemplate: TransactionTemplate =
    transactionManager.getOrElse(null) match {
      case txMan: PlatformTransactionManager => new TransactionTemplate(txMan)
      case null => null
    }


  private def doInTransaction[T](callback: () => T): T = {
    val transactionCallback = new TransactionCallback[T] {
      def doInTransaction(p1: TransactionStatus) = callback()
    }
    val resultOfTransaction = if (transactionTemplate == null) {
      transactionTemplate.execute(transactionCallback)
    } else {
      transactionCallback.doInTransaction(null)
    }
    resultOfTransaction
  }

  def enqueue(receiver: ActorRef, handle: Envelope) {
    doInTransaction( () => {
      if (exchange != null)
        rabbitTemplate.convertAndSend(exchange, routingKey, handle)
      else {
        rabbitTemplate.convertAndSend(routingKey, handle)
      }
    })

  }

  def dequeue() = new Envelope(rabbitTemplate.receive(this.queueName), owner.self)


  def numberOfMessages = 0

  def hasMessages = false

  def systemEnqueue(receiver: ActorRef, message: SystemMessage) {}

  def systemDrain() = null

  def hasSystemMessages = false

  def afterPropertiesSet() {

  }
}