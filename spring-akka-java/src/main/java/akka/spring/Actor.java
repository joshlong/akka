package akka.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Signifies that a component is to be handled by Spring Akka
 *
 * This is simply a stereotype although,
 * the interface itself may be used to signal
 * the inclusion of certain default behaviors.
 *
 * Additionally, this annotation may be used to
 * introduce a certain, special, "Actor" scope
 * for beans. This could be a way of binding a
 * bean to a local Actor, only.
 *
 * Consider this a more powerful version of a
 * "threadlocal", except, you know, an "actorlocal."
 *
 * @author Josh Long
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Actor {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean in case of an autodetected component.
	 * @return the suggested component name, if any
	 */
	String value() default "";

}
