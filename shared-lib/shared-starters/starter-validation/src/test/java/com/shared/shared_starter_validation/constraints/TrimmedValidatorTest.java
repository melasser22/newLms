package com.shared.shared_starter_validation.constraints;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrimmedValidatorTest {
  private final TrimmedValidator validator = new TrimmedValidator();

  @Test
  void rejectsWhitespace() {
    assertFalse(validator.isValid("  abc ", null));
  }

  @Test
  void acceptsTrimmed() {
    assertTrue(validator.isValid("abc", null));
  }
}
