package akka.spring;

import java.lang.annotation.*;

 


/**
 * Annotation that signals that a reference to the "self" ActorReference should be specified
 *
 * @author Josh Long
 */

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Self {
}
