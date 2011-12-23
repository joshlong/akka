package akka.spring;

import java.lang.annotation.*;

/**
 * Annotation that signals that a reference to the "context" ActorContext should be specified
 *
 * @author Josh Long
 */

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Context {
}
