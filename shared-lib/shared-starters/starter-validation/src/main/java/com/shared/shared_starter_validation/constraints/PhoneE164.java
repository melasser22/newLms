package com.shared.shared_starter_validation.constraints;
import jakarta.validation.Constraint; import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER}) @Retention(RetentionPolicy.RUNTIME) @Constraint(validatedBy=PhoneE164Validator.class)
public @interface PhoneE164 { String message() default "must be E.164 (e.g., +15551234567)"; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }
