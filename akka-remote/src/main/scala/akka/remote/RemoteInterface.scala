/**
 * Copyright (C) 2009-2010 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.remote

import akka.actor._
import akka.AkkaException
import scala.reflect.BeanProperty
import java.io.{ PrintWriter, PrintStream }
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.InetAddress
import java.net.UnknownHostException
import java.net.UnknownServiceException

/**
 * Interface for remote transports to encode their addresses. The three parts
 * are named according to the URI spec (precisely java.net.URI) which is used
 * for parsing. That means that the address’ parts must conform to what an
 * URI expects, but otherwise each transport may assign a different meaning
 * to these parts.
 */
trait RemoteTransportAddress {
  def protocol: String
  def host: String
  def port: Int
}

trait ParsedTransportAddress extends RemoteTransportAddress

case class RemoteNettyAddress(host: String, ip: Option[InetAddress], port: Int) extends ParsedTransportAddress {
  def protocol = "akka"
}

object RemoteNettyAddress {
  def apply(host: String, port: Int): RemoteNettyAddress = {
    // FIXME this may BLOCK for extended periods of time!
    val ip = try Some(InetAddress.getByName(host)) catch { case _: UnknownHostException ⇒ None }
    new RemoteNettyAddress(host, ip, port)
  }
  def apply(s: String): RemoteNettyAddress = {
    val RE = """([^:]+):(\d+)""".r
    s match {
      case RE(h, p) ⇒ apply(h, Integer.parseInt(p))
      case _        ⇒ throw new IllegalArgumentException("cannot parse " + s + " as <host:port>")
    }
  }
}

case class UnparsedTransportAddress(protocol: String, host: String, port: Int) extends RemoteTransportAddress {
  def parse(transports: TransportsMap): RemoteTransportAddress =
    transports.get(protocol)
      .map(_(host, port))
      .toRight("protocol " + protocol + " not known")
      .joinRight.fold(UnparseableTransportAddress(protocol, host, port, _), identity)
}

case class UnparseableTransportAddress(protocol: String, host: String, port: Int, error: String) extends RemoteTransportAddress

case class RemoteSystemAddress[+T <: ParsedTransportAddress](system: String, transport: T) extends Address {
  def protocol = transport.protocol
  @transient
  lazy val hostPort = system + "@" + transport.host + ":" + transport.port
}

case class UnparsedSystemAddress[+T <: RemoteTransportAddress](system: Option[String], transport: T) {
  def parse(transports: TransportsMap): Either[UnparsedSystemAddress[UnparseableTransportAddress], RemoteSystemAddress[ParsedTransportAddress]] =
    system match {
      case Some(sys) ⇒
        transport match {
          case x: ParsedTransportAddress ⇒ Right(RemoteSystemAddress(sys, x))
          case y: UnparsedTransportAddress ⇒
            y.parse(transports) match {
              case x: ParsedTransportAddress      ⇒ Right(RemoteSystemAddress(sys, x))
              case y: UnparseableTransportAddress ⇒ Left(UnparsedSystemAddress(system, y))
              case z                              ⇒ Left(UnparsedSystemAddress(system, UnparseableTransportAddress(z.protocol, z.host, z.port, "cannot parse " + z)))
            }
          case z ⇒ Left(UnparsedSystemAddress(system, UnparseableTransportAddress(z.protocol, z.host, z.port, "cannot parse " + z)))
        }
      case None ⇒ Left(UnparsedSystemAddress(None, UnparseableTransportAddress(transport.protocol, transport.host, transport.port, "no system name specified")))
    }
}

object RemoteAddressExtractor {
  def unapply(s: String): Option[UnparsedSystemAddress[UnparsedTransportAddress]] = {
    try {
      val uri = new URI(s)
      if (uri.getScheme == null || uri.getHost == null || uri.getPort == -1) None
      else Some(UnparsedSystemAddress(Option(uri.getUserInfo), UnparsedTransportAddress(uri.getScheme, uri.getHost, uri.getPort)))
    } catch {
      case _: URISyntaxException ⇒ None
    }
  }
}

object RemoteActorPath {
  def unapply(addr: String): Option[(UnparsedSystemAddress[UnparsedTransportAddress], Iterable[String])] = {
    try {
      val uri = new URI(addr)
      if (uri.getScheme == null || uri.getUserInfo == null || uri.getHost == null || uri.getPort == -1 || uri.getPath == null) None
      else Some(UnparsedSystemAddress(Some(uri.getUserInfo), UnparsedTransportAddress(uri.getScheme, uri.getHost, uri.getPort)),
        ActorPath.split(uri.getPath).drop(1))
    } catch {
      case _: URISyntaxException ⇒ None
    }
  }
}

object ParsedActorPath {
  def unapply(addr: String)(implicit transports: TransportsMap): Option[(RemoteSystemAddress[ParsedTransportAddress], Iterable[String])] = {
    try {
      val uri = new URI(addr)
      if (uri.getScheme == null || uri.getUserInfo == null || uri.getHost == null || uri.getPort == -1 || uri.getPath == null) None
      else
        UnparsedSystemAddress(Some(uri.getUserInfo), UnparsedTransportAddress(uri.getScheme, uri.getHost, uri.getPort)).parse(transports) match {
          case Left(_)  ⇒ None
          case Right(x) ⇒ Some(x, ActorPath.split(uri.getPath).drop(1))
        }
    } catch {
      case _: URISyntaxException ⇒ None
    }
  }
}

class RemoteException(message: String) extends AkkaException(message)

trait RemoteModule {
  protected[akka] def notifyListeners(message: RemoteLifeCycleEvent): Unit
}

/**
 * Remote life-cycle events.
 */
sealed trait RemoteLifeCycleEvent

/**
 * Life-cycle events for RemoteClient.
 */
trait RemoteClientLifeCycleEvent extends RemoteLifeCycleEvent {
  def remoteAddress: ParsedTransportAddress
}

case class RemoteClientError[T <: ParsedTransportAddress](
  @BeanProperty cause: Throwable,
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

case class RemoteClientDisconnected[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

case class RemoteClientConnected[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

case class RemoteClientStarted[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

case class RemoteClientShutdown[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

case class RemoteClientWriteFailed[T <: ParsedTransportAddress](
  @BeanProperty request: AnyRef,
  @BeanProperty cause: Throwable,
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty remoteAddress: T) extends RemoteClientLifeCycleEvent

/**
 *  Life-cycle events for RemoteServer.
 */
trait RemoteServerLifeCycleEvent extends RemoteLifeCycleEvent

case class RemoteServerStarted[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerShutdown[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerError[T <: ParsedTransportAddress](
  @BeanProperty val cause: Throwable,
  @BeanProperty remote: RemoteSupport[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerClientConnected[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty val clientAddress: Option[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerClientDisconnected[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty val clientAddress: Option[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerClientClosed[T <: ParsedTransportAddress](
  @BeanProperty remote: RemoteSupport[T],
  @BeanProperty val clientAddress: Option[T]) extends RemoteServerLifeCycleEvent
case class RemoteServerWriteFailed[T <: ParsedTransportAddress](
  @BeanProperty request: AnyRef,
  @BeanProperty cause: Throwable,
  @BeanProperty server: RemoteSupport[T],
  @BeanProperty remoteAddress: Option[T]) extends RemoteServerLifeCycleEvent

/**
 * Thrown for example when trying to send a message using a RemoteClient that is either not started or shut down.
 */
class RemoteClientException[T <: ParsedTransportAddress] private[akka] (
  message: String,
  @BeanProperty val client: RemoteSupport[T],
  val remoteAddress: T, cause: Throwable = null) extends AkkaException(message, cause)

/**
 * Thrown when the remote server actor dispatching fails for some reason.
 */
class RemoteServerException private[akka] (message: String) extends AkkaException(message)

/**
 * Thrown when a remote exception sent over the wire cannot be loaded and instantiated
 */
case class CannotInstantiateRemoteExceptionDueToRemoteProtocolParsingErrorException private[akka] (cause: Throwable, originalClassName: String, originalMessage: String)
  extends AkkaException("\nParsingError[%s]\nOriginalException[%s]\nOriginalMessage[%s]"
    .format(cause.toString, originalClassName, originalMessage)) {
  override def printStackTrace = cause.printStackTrace
  override def printStackTrace(printStream: PrintStream) = cause.printStackTrace(printStream)
  override def printStackTrace(printWriter: PrintWriter) = cause.printStackTrace(printWriter)
}

abstract class RemoteSupport[-T <: ParsedTransportAddress](val system: ActorSystemImpl) {
  /**
   * Shuts down the remoting
   */
  def shutdown(): Unit

  /**
   *  Gets the name of the server instance
   */
  def name: String

  /**
   *  Starts up the remoting
   */
  def start(loader: Option[ClassLoader]): Unit

  /**
   * Shuts down a specific client connected to the supplied remote address returns true if successful
   */
  def shutdownClientConnection(address: T): Boolean

  /**
   * Restarts a specific client connected to the supplied remote address, but only if the client is not shut down
   */
  def restartClientConnection(address: T): Boolean

  /** Methods that needs to be implemented by a transport **/

  protected[akka] def send(message: Any,
                           senderOption: Option[ActorRef],
                           recipient: RemoteActorRef,
                           loader: Option[ClassLoader]): Unit

  protected[akka] def notifyListeners(message: RemoteLifeCycleEvent): Unit = system.eventStream.publish(message)

  override def toString = name
}
