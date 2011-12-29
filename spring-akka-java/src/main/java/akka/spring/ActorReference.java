package akka.spring;

import org.springframework.stereotype.Component;
import akka.actor.Actors;
import java.lang.annotation.*;

/**
 * Annotation used to mark injection sites for the Akka {@link Actors}.
 *
 * @author Josh Long
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ActorReference {
    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an auto-detected component.
     * @return the suggested component name, if any. Expects a selector expression that will 
     * ultimately be passed to the Akka subsystem to get a selector  
     */
    String value() default ""; 
}
