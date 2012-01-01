package akka.spring

import actors.NoisyActor
import mailbox.RabbitMqMailbox
import org.springframework.context.annotation.{Bean, Configuration, ComponentScan}


@ComponentScan(basePackageClasses = Array[Class[_]](classOf[NoisyActor]))
@Configuration
class ActorConfiguration {
  
  @Bean def mailbox = {
    val rmqmb  = new RabbitMqMailbox( )
    rmqmb
  }
  
  @Bean 
  def actorSystem =  {
    val as = new ActorSystemFactoryBean
    as.setMailbox( this.mailbox )
    as 
  }

}
