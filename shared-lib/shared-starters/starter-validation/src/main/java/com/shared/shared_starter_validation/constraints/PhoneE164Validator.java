package com.shared.shared_starter_validation.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneE164Validator implements ConstraintValidator<PhoneE164, String> {

    // E.164: "+" followed by up to 15 digits, first digit 1–9
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Treat null/blank as "not this validator's job"; use @NotBlank if required
        if (value == null || value.isBlank()) return true;
        return E164.matcher(value).matches();
    }
}
