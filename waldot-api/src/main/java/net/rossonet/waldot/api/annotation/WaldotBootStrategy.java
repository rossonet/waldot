package net.rossonet.waldot.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a Waldot Boot Strategy. This annotation must be
 * used to indicate that the class contains a strategy for booting Waldot,
 * typically for initializing rules or configurations.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WaldotBootStrategy {

}
