/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */

package akka

package object actor {
  implicit def actorRef2Scala(ref: ActorRef): ScalaActorRef = ref.asInstanceOf[ScalaActorRef]
  implicit def scala2ActorRef(ref: ScalaActorRef): ActorRef = ref.asInstanceOf[ActorRef]

  type Uuid = com.eaio.uuid.UUID

  def newUuid(): Uuid = new Uuid()

  def uuidFrom(time: Long, clockSeqAndNode: Long): Uuid = new Uuid(time, clockSeqAndNode)

  def uuidFrom(uuid: String): Uuid = new Uuid(uuid)

  def simpleName(obj: AnyRef): String = {
    val n = obj.getClass.getName
    val i = n.lastIndexOf('.')
    n.substring(i + 1)
  }

  def simpleName(clazz: Class[_]): String = {
    val n = clazz.getName
    val i = n.lastIndexOf('.')
    n.substring(i + 1)
  }

  implicit def future2actor[T](f: akka.dispatch.Future[T]) = new {
    def pipeTo(actor: ActorRef): this.type = {
      f onComplete {
        case Right(r) ⇒ actor ! r
        case Left(f)  ⇒ actor ! Status.Failure(f)
      }
      this
    }
  }

}
