package net.rossonet.zenoh.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface ExportedObject {
	String description() default "";

	String name() default "";

	boolean unique() default true;

}
