package eu.miltema.slimorm;

import java.lang.annotation.*;

/**
 * Indicates that this field will be stored as Json in database
 *
 * @author Margus
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JSon {

}
