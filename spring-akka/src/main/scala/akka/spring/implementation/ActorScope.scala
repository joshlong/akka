package akka.spring.implementation

import org.springframework.beans.factory.config.Scope
import org.springframework.beans.factory.ObjectFactory


/**
 * A stm scope that lets you bind values to the current Actor <code>receive</CODE> block.
 */
class ActorScope extends Scope {
  def get(name: String, objectFactory: ObjectFactory[_]) = null

  def remove(name: String) = null

  def registerDestructionCallback(name: String, callback: Runnable) {}

  def resolveContextualObject(key: String) = null

  def getConversationId = ""
}