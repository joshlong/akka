package akka.spring.config

import org.springframework.util.Assert
import akka.actor.{ActorRef, ActorContext, Actor}
import util.Log._
import collection.mutable.HashMap
import util.{Argument, HandlerMetadata, ComponentReflectionUtilities}

class DelegatingActor(delegate: AnyRef) extends Actor {

  val receiveAnnotation = classOf[akka.spring.Receive]
  var handlers: List[HandlerMetadata] = _
  setup(delegate);

  private def setup(b: AnyRef) {
    handlers = ComponentReflectionUtilities.findMethodsWithAnnotation(b, receiveAnnotation)
    Assert.notNull(handlers.size > 0, "there must be at least one method annotated with " + receiveAnnotation.getClass.getName)
  }

  protected def doReceive(msg: AnyRef) {

    val c: ActorContext = this.context
    val s: ActorRef = this.self

    log("inside the doReceive method about to invoke a handler method " + this.handlers.length);

    // lets find the right handler method that accepts a payload of the same type as specified here

    this.handlers.foreach((handler: HandlerMetadata) => {

      val mapOfArgs = new HashMap[Int, AnyRef]

      handler.payload.get match {
        case null => throw new RuntimeException("You must have a parameter that accepts the payload of the message! ")
        case ar: Argument => {
          // build a map that takes integer arg poisitions as keys
          // then find the highest integer key, N
          // then iterate through, 0->N, and add the values for those keys to an Array
          // then use the array to invoke the handler receive method

          log("handler's payload class: " + ar.argumentType.getName + "; actual message class: " + msg.getClass.getName)

          if ((ar.argumentType).isAssignableFrom(msg.getClass)) {
            // then this is the handler method we should call!
            mapOfArgs += ar.argumentPosition -> msg;

            handler.selfReference.getOrElse(null) match {
              case null => null
              case selfArg: Argument => {
                mapOfArgs += selfArg.argumentPosition -> s
              }

            };

            handler.actorContextReference.getOrElse(null) match {
              case null => null
              case acRef: Argument => {
                mapOfArgs += acRef.argumentPosition -> c
              }
            }

            var args = List[AnyRef]()
            // now, iterate through the map and then build an array and actually invoke the method
            mapOfArgs.keySet.toList.sorted.foreach((k: Int) => {
              args = mapOfArgs(k) :: args
            })

            val argsForInvocation = args.reverse;
            log(argsForInvocation.toString())
            // todo fix me

            try {

              ActorLocalStorage.current.get() match {
                case null => ActorLocalStorage.current.set(new ActorLocalStorage(s, c))
              }
              handler.method.invoke(this.delegate, argsForInvocation: _*) // the '_*' tells scala that we want to use this sequence as a varargs expansion
            } finally {
              ActorLocalStorage.current.remove()
            }
          }
        }
      }


    });
  }


  protected def receive = {
    case e: AnyRef => doReceive(e)
  }
}