package estivate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Get the string contents of the document's title element
 * 
 * @author Benoit Theunissen
 *
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface Title {

    boolean optional() default false;

}
