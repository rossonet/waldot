package net.rossonet.zenoh.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface ExportedCommand {

	String description();

	String name();

	String returnDescription();

	String returnName();

}
