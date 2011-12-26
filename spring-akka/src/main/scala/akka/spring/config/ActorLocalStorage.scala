package akka.spring.config

import akka.actor.{ActorContext, ActorRef}

class ActorLocalStorage(var self: ActorRef, var context: ActorContext)

object ActorLocalStorage {
  val current = new ThreadLocal[ActorLocalStorage]();
}
