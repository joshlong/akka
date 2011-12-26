
package akka.spring.implementation;

import akka.actor._
import org.springframework.beans.factory.FactoryBean

class ActorSystemFactoryBean extends FactoryBean[ActorSystem] {

  def isSingleton = true

  def getObjectType = classOf[ActorSystem]

  def getObject = ActorSystem()
}
