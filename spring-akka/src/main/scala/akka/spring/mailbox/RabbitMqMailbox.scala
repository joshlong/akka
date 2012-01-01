package akka.spring.mailbox

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.core.{Message, MessagePostProcessor}
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate}
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}
import akka.actor.{ActorRef, ActorContext}
import akka.dispatch.{SystemMessage, Envelope, MessageQueue, CustomMailbox}
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager


/**
 * Simple Akka Mailbox that delegates to RabbitMQ. There should be little reason that generic AMQP couldn't be used instead.
 *
 * Patterned after https://github.com/jboner/akka/blob/master/akka-durable-mailboxes/akka-redis-mailbox/src/main/scala/akka/actor/mailbox/RedisBasedMailbox.scala 
 *
 * @author Josh Long
 */
class RabbitMqMailbox( owner: ActorContext,
                       routingKey: String = "",
                       exchange: String,
                       queue: String,
                       rabbitConnection: ConnectionFactory,
                       var transactionManager: PlatformTransactionManager ,
                       messagePostProcessor: MessagePostProcessor = new MessagePostProcessor {
                         def postProcessMessage(message: Message) = message
                       })
  extends CustomMailbox(owner) with MessageQueue {


  if (transactionManager == null)
    transactionManager = new RabbitTransactionManager( rabbitConnection)

  val transactionTemplate = new TransactionTemplate(transactionManager)

  val sendFunction = rabbitTemplate.convertAndSend(this.exchange, this.routingKey, _: Any, this.messagePostProcessor)

  val rabbitTemplate = {
    val rt = new RabbitTemplate(this.rabbitConnection)
    rt.afterPropertiesSet()
    rt
  }

  private def doInTransaction[T](callback: => T): T = {
    transactionTemplate.execute(new TransactionCallback[T] {
      def doInTransaction(p1: TransactionStatus) = callback
    })
  }

  def enqueue(receiver: ActorRef, handle: Envelope) {
    doInTransaction(sendFunction(handle));
  }

  def dequeue() = doInTransaction[Envelope](rabbitTemplate.receiveAndConvert(this.queue).asInstanceOf[Envelope])

  def numberOfMessages = -1 // is there a safer way to do this?   

  def hasMessages = numberOfMessages > 1 || numberOfMessages == -1

  def systemEnqueue(receiver: ActorRef, message: SystemMessage) {}

  def systemDrain() = null

  def hasSystemMessages = false


}