package akka.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annnotation used to mark injection sites for the Akka {@link ActorSystem}.
 *
 * @author Josh Long
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ActorSystem {
  // no attributes are provided as there is only one, global reference to the ActorSystem
}
