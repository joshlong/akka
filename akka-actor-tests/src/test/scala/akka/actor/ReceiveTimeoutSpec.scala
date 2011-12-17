/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor

import akka.testkit._
import akka.util.duration._

import java.util.concurrent.atomic.AtomicInteger

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ReceiveTimeoutSpec extends AkkaSpec {

  "An actor with receive timeout" must {

    "get timeout" in {
      val timeoutLatch = TestLatch()

      val timeoutActor = system.actorOf(Props(new Actor {
        context.setReceiveTimeout(500 milliseconds)

        protected def receive = {
          case ReceiveTimeout ⇒ timeoutLatch.open
        }
      }))

      timeoutLatch.await
      system.stop(timeoutActor)
    }

    "reschedule timeout after regular receive" in {
      val timeoutLatch = TestLatch()
      case object Tick

      val timeoutActor = system.actorOf(Props(new Actor {
        context.setReceiveTimeout(500 milliseconds)

        protected def receive = {
          case Tick           ⇒ ()
          case ReceiveTimeout ⇒ timeoutLatch.open
        }
      }))

      timeoutActor ! Tick

      timeoutLatch.await
      system.stop(timeoutActor)
    }

    "be able to turn off timeout if desired" in {
      val count = new AtomicInteger(0)
      val timeoutLatch = TestLatch()
      case object Tick

      val timeoutActor = system.actorOf(Props(new Actor {
        context.setReceiveTimeout(500 milliseconds)

        protected def receive = {
          case Tick ⇒ ()
          case ReceiveTimeout ⇒
            count.incrementAndGet
            timeoutLatch.open
            context.resetReceiveTimeout()
        }
      }))

      timeoutActor ! Tick

      timeoutLatch.await
      count.get must be(1)
      system.stop(timeoutActor)
    }

    "not receive timeout message when not specified" in {
      val timeoutLatch = TestLatch()

      val timeoutActor = system.actorOf(Props(new Actor {
        protected def receive = {
          case ReceiveTimeout ⇒ timeoutLatch.open
        }
      }))

      timeoutLatch.awaitTimeout(1 second) // timeout expected
      system.stop(timeoutActor)
    }

    "have ReceiveTimeout eq to Actors ReceiveTimeout" in {
      akka.actor.Actors.receiveTimeout must be theSameInstanceAs (ReceiveTimeout)
    }
  }
}
