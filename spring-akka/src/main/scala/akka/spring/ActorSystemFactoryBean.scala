package akka.spring

import akka.actor._
import mailbox.{DelegatingUnmanagedMailboxType, DelegatingUnmanagedMailbox}
import org.springframework.core.io.Resource
import org.springframework.util.Assert._
import org.springframework.beans.factory.{InitializingBean, FactoryBean}
import com.typesafe.config.{ConfigFactory, Config}
import reflect.BeanProperty
import java.io.{Reader, InputStreamReader}
import org.springframework.util.StringUtils
import akka.dispatch.Mailbox

/**
 *
 * Factory bean used configure an Akka [[akka.actor.ActorSystem]], which is at the heart of the Akka
 * runtime. If none is specified, then a default <CODE>ActorSystem</CODE> will be generated.
 *
 * @author Josh Long
 * @since 2.0
 */
class ActorSystemFactoryBean extends FactoryBean[ActorSystem] with InitializingBean   {

  private var config: Config = _

  def afterPropertiesSet() {

    isTrue(StringUtils.hasText(this.configurationString) || StringUtils.hasText(resourceBaseName) || this.configuration != null,
      "you must specify the 'configurationString' or a 'resourceBaseName' or a 'configuration' property (but only one!)")

    if (this.configurationString == null)
      this.configurationString = "";

    if (this.mailbox != null) {
      DelegatingUnmanagedMailbox.mailbox = this.mailbox
      val extraConfigurationString = String.format("""
        akka { 
          actor  { 
           default-dispatcher { 
              mailboxType = "%s"
           } 
          }
        } 
      """, classOf[DelegatingUnmanagedMailboxType].getName);
      this.configurationString += extraConfigurationString
    }

    // first one has just the defaults
    this.config = ConfigFactory.load();

    if (StringUtils.hasText(this.resourceBaseName)) {
      this.config.withFallback(ConfigFactory.load(this.resourceBaseName))
    }

    if (this.configuration != null) {
      // load the Spring io.Resource as a configuration file
      var is: Reader = null
      try {
        is = new InputStreamReader(this.configuration.getInputStream)
        this.config.withFallback( ConfigFactory.parseReader(is))
      } finally {
        if (is != null)
          is.close()
      }
    }

    if (StringUtils.hasText(this.configurationString)) {
      config.withFallback(ConfigFactory.parseString(this.configurationString))
    }
  }

  /**
   * Used the default loading process, but change the base name. By default the application loads
   * <CODE>application.(conf,json,properties)</CODE>.
   */
  @BeanProperty var resourceBaseName: String = "application"

  /**
   * Path to the configuration file to configure Akka. For an example, see
   * <CODE>classpath:</CODE>
   */
  @BeanProperty var configuration: Resource = _

  /**
   * Alternatively, you may specify the configuration as a String when constructing the [[akka.actor.ActorSystem]]
   */
  @BeanProperty var configurationString: String = _

  /**
   * The name of the configuration itself
   */
  @BeanProperty var configName = getClass.getSimpleName + "Config"

  /**
   * A reference to a mailbox. the only reason you should configure the mailbox here instead of specifying the FQCN in the config file format
   */
  @BeanProperty var mailbox: Mailbox = _


  def isSingleton = true

  def getObjectType = classOf[ActorSystem]

  def getObject = ActorSystem(this.configName, this.config)
}
