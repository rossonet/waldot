package net.rossonet.zenoh.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ExportedMethodParameter {
	boolean advancedConfigurationField() default false;

	String defaultValue() default "";

	String description() default "";

	String fieldValidationRegEx() default ".*";

	boolean isArray() default false;

	boolean mandatary() default false;

	String mimeType() default "text/plain";

	String name() default "";

	String[] parameters() default {};

	String permissions() default "all"; // TODO: definire meglio

	boolean textArea() default false;

	String unit() default "text";

	int viewOrder() default 10;

	boolean writable() default true;
}
