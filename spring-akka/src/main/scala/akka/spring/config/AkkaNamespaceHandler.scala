package akka.spring.config

/**
 *  Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */
import org.springframework.beans.factory.xml.NamespaceHandlerSupport


/**
 * Supports parsing the Akka Spring namespace. The namespace (at least initially)
 * will be minimal. The goal is not to reproduce every possible configuration option,
 * but instead to make it easy for Akka actors to play in the Spring container.
 *
 * The most natural, and first use case is &lt;akka:annotation-driven/&gt;
 * with a corresponding annotation in Spring 3.1
 *
 * @author Josh Long
 */
class AkkaNamespaceHandler extends NamespaceHandlerSupport {

  def init() {

  }
}