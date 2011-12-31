package a.stm.example

import akka.dispatch.Await
import akka.util.duration._
import akka.util.Timeout
import akka.actor.{Props, ActorSystem}
import akka.transactor.Coordinated

object CoordinatedRegistry {

}

object StmExample extends App {

  def doInTransaction[T](callback: (Coordinated) => T) {
    val coordinated = Coordinated()
  }

  val system = ActorSystem()

  val counter1 = system.actorOf(Props[SharedCounter], name = "counter1")
  val counter2 = system.actorOf(Props[SharedCounter], name = "counter2")

  implicit val timeout = Timeout(5 seconds)

  counter1 ! Coordinated(Increment(Some(counter2)))

  val count1 = Await.result(counter1 ? GetCount, timeout.duration)

  val count2 = Await.result(counter2 ? GetCount, timeout.duration)
  Console.println(count1 + " : " + count2)
}


