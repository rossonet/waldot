package net.rossonet.waldot.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WaldotHistoryStrategy {

}
