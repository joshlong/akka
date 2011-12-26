package akka.spring.config.util

import reflect.BeanProperty
import java.lang.reflect.Method


class HandlerMetadata {
  @BeanProperty var method: Method = _
  @BeanProperty var selfReference: Option[Argument] = None
  @BeanProperty var actorContextReference: Option[Argument] = None
  @BeanProperty var payload: Option[Argument] = None
}
