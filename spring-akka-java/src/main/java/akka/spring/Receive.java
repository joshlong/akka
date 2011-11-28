package akka.spring;

import java.lang.annotation.*;

/**
 * Marks a method as a receive callback
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Receive {
}
