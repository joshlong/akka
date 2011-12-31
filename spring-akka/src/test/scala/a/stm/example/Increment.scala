package a.stm.example

import akka.actor.ActorRef


case class Increment(friend: Option[ActorRef] = None)





