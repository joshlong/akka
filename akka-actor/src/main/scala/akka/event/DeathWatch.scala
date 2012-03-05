/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.event

import akka.actor._

/**
 * The contract of DeathWatch is not properly expressed using the type system
 * Whenever there is a publish, all listeners to the Terminated Actor should be atomically removed
 * A failed subscribe should also only mean that the Classifier (ActorRef) that is listened to is already shut down
 * See LocalDeathWatch for semantics
 */
trait DeathWatch extends ActorEventBus with ActorClassifier {
  type Event = Terminated

  protected final def classify(event: Event): Classifier = event.actor
}
<<<<<<< HEAD
*/

/*
* Scenarios that can occur:
*
* Child dies without supervisor (will perhaps not be possible)
* Child dies whose supervisor is dead (race)
* Child dies, supervisor cannot deal with the problem and has no supervisor (will perhaps not be possible)
* Child dies, supervisor cannot deal with the problem and its supervisor is dead (race)
* Child dies, supervisor can deal with it: AllForOnePermanentStrategy
* Child dies, supervisor can deal with it: AllForOnePermanentStrategy but has reached max restart quota for child
* Child dies, supervisor can deal with it: AllForOneTemporaryStrategy
* Multiple children dies, supervisor can deal with it: AllForOnePermanentStrategy
* Multiple children dies, supervisor can deal with it: AllForOneTemporaryStrategy
* Child dies, supervisor can deal with it: OneForOnePermanentStrategy
* Child dies, supervisor can deal with it: OneForOneTemporaryStrategy
*
* Things that should be cleared after restart
*   - monitored children (not supervised)
*
* Things that should be cleared after resume
*   - nothing
*
* Things that should be cleared after death
*   - everything
*
* Default implementation of preRestart == postStop
* Default implementation of postRestart == preStart
*
* */ 
=======
>>>>>>> master
