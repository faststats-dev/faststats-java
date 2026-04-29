package dev.faststats.data;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * An annotation to mark a source id.
 *
 * @since 0.23.0
 */
@NonNls
@Pattern(SourceId.PATTERN)
@Retention(RetentionPolicy.CLASS)
@Target({METHOD, FIELD, PARAMETER, LOCAL_VARIABLE})
public @interface SourceId {
    String PATTERN = "[a-z_]+";
}
