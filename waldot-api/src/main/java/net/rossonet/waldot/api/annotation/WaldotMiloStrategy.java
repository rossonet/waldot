package net.rossonet.waldot.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a Waldot Milo Strategy. This annotation must be
 * used to indicate that the class implements the logic to handle the Gremlin
 * objects as OPC UA nodes
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WaldotMiloStrategy {

}
