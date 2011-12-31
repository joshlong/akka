package a.stm.example

import akka.actor.Actor
import scala.concurrent.stm._
import akka.transactor.Coordinated

class SharedCounter extends Actor {

  val count = Ref(0)

  def receive = {
    case coordinated @ Coordinated(Increment(friend)) ⇒ {
      friend foreach (_ ! coordinated(Increment(None)))
      coordinated atomic { implicit t ⇒ count transform (_ + 1) }
    }
    case GetCount => this.sender ! count.single.get
  }
}


