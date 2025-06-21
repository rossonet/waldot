package net.rossonet.waldot.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a Waldot Agent Management Strategy. This
 * annotation can be used to indicate that the class implements a specific
 * strategy for managing Waldot agents.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WaldotAgentManagementStrategy {

}
