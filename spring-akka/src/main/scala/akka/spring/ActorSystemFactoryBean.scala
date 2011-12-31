package akka.spring

import akka.actor._
import org.springframework.core.io.Resource
import org.springframework.util.Assert._
import org.springframework.beans.factory.{InitializingBean, FactoryBean}
import com.typesafe.config.{ConfigFactory, Config}
import reflect.BeanProperty
import java.io.{Reader, InputStreamReader}
import org.springframework.util.StringUtils

/**
 *
 * Factory bean used configure an Akka [[akka.actor.ActorSystem]], which is at the heart of the Akka
 * runtime. If none is specified, then a default <CODE>ActorSystem</CODE> will be generated.
 *
 * @author Josh Long
 * @since 2.0
 */
class ActorSystemFactoryBean extends FactoryBean[ActorSystem] with InitializingBean {

  private var config: Config = _

  def afterPropertiesSet() {

    isTrue(StringUtils.hasText(this.configurationString) || StringUtils.hasText(resourceBaseName) || this.configuration != null,
      "you must specify the 'configurationString' or a 'resourceBaseName' or a 'configuration' property (but only one!)")

    if (configuration == null) {
      notNull(configurationString, "the configurationString can't be null")
      hasText(configurationString, "the configurationString, if specified, should not be empty")
    }
    this.config =
      if (StringUtils.hasText(this.resourceBaseName)) {
        ConfigFactory.load(this.resourceBaseName)
      }
      else if (this.configuration != null) {
        // load the Spring io.Resource as a configuration file
        var is: Reader = null
        try {
          is = new InputStreamReader(this.configuration.getInputStream)
          ConfigFactory.parseReader(is)
        } finally {
          if (is != null)
            is.close()
        }
      } else if (StringUtils.hasText(this.configurationString)) {
        // load the configuration string
        ConfigFactory.parseString(this.configurationString)
      }
      else {
        // fallback case
        ConfigFactory.load()
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


  def isSingleton = true

  def getObjectType = classOf[ActorSystem]

  def getObject = ActorSystem(this.configName, this.config)
}
