package com.shared.shared_starter_validation.constraints;
import jakarta.validation.Constraint; import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER}) @Retention(RetentionPolicy.RUNTIME) @Constraint(validatedBy=TrimmedValidator.class)
public @interface Trimmed { String message() default "must be trimmed"; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }
