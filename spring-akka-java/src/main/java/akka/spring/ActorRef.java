package akka.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annnotation used to mark injection sites for the Akka {@link ActorRef}.
 *
 * @author Josh Long
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ActorRef {
    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * @return the suggested component name, if any. Expects a selector expression that will 
     * ultimately be passed to {@link ActorSystemImpl#actorFor(String }
     */
    String value() default ""; 
}
