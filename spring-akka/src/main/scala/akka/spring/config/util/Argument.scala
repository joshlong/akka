package akka.spring.config.util


import reflect.BeanProperty
import java.lang.annotation.Annotation

/**
 * stores information about a single argument to a handler method
 */
class Argument(@BeanProperty var argumentType: Class[_], @BeanProperty var argumentPosition: Int, @BeanProperty var argumentAnnotation: Annotation)