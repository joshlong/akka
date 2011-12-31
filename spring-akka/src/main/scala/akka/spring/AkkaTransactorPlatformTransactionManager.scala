package akka.spring

import org.springframework.transaction.{TransactionStatus, TransactionDefinition, PlatformTransactionManager}
import scala.concurrent.stm._


/***
 *
 * Implementation of Spring's [[org.springframework.transaction.PlatformTransactionManager]] interface backed by Scala 2.9.1's STM
 * (the module was removed from Akka in the 2.0 release an incorporated as part of the core language from Akka 2.0).
 *
 * To use this transaction manager, declare the [[akka.spring.AkkaTransactorPlatformTransactionManager]] in your
 * Spring configuration and then either turn on declarative transaction management
 * (<CODE><tx:annotation-driven transaction-manager = "akkaStmTransactionManager"/></CODE> or <CODE>@EnableTransactionManagement</CODE>). *
 *
 * This transaction manager works on the principles of STM. For more, see <A HREF ="http://akka.io/docs/akka/snapshot/scala/transactors.html">this explanation of transactors</A>.
 *
 * @author Josh Long
 * @since 2.0
 */
class AkkaTransactorPlatformTransactionManager extends PlatformTransactionManager {

  def commit(p1: TransactionStatus) {}

  def rollback(p1: TransactionStatus) {}

  def getTransaction(p1: TransactionDefinition) = null
}