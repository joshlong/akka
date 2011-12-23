package akka.spring;


import java.lang.annotation.*;

/**
 * Annotation that can be used to specify that a parameter is to be used to receivee the payload of a
 * message sent to an actor's mailbox. 
 * 
 * If the handler method only has one parameter, then it is assumed that the parameter is intended
 * to rdceive the payload, and thus needs not have this annotation present.
 * 
 * @author Josh Long
 */

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload {
}
