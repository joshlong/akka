package akka.spring

import actors.NoisyActor
import org.springframework.context.annotation.{Configuration, ComponentScan}


@ComponentScan(basePackageClasses = Array[Class[_]](classOf[NoisyActor]))
@Configuration
class ActorConfiguration
