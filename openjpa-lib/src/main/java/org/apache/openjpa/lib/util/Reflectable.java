package org.apache.openjpa.lib.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a getter method or field so {@link Reflection reflection 
 * utility} to control whether the annotated member is recorded during scanning 
 * for bean-style method or field.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Reflectable {
    boolean value() default true;
}
