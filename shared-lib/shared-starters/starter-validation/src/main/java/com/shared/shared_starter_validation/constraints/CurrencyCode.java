package com.shared.shared_starter_validation.constraints;
import jakarta.validation.Constraint; import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER}) @Retention(RetentionPolicy.RUNTIME) @Constraint(validatedBy=CurrencyCodeValidator.class)
public @interface CurrencyCode { String message() default "invalid ISO 4217 currency"; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }
