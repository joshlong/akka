package akka.spring.config


import akka.actor._
import org.springframework.beans.factory.FactoryBean


// todo make this more impressive, and make it support all the options that the config file format supports.
class ActorSystemFactoryBean extends FactoryBean[ActorSystem] {

  def isSingleton = true

  def getObjectType = classOf[ActorSystem]

  def getObject = ActorSystem()
}
