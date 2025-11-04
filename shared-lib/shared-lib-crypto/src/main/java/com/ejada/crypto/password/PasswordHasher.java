package com.ejada.crypto.password;

import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class that centralises password hashing routines so services do not
 * need to instantiate their own encoders.  By delegating to Spring Security's
 * {@link BCryptPasswordEncoder} we ensure passwords are hashed following
 * industry best practices with a random salt per value.
 */
public final class PasswordHasher {

  private static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder();

  private PasswordHasher() {
    // utility class
  }

  /**
   * Hashes the provided password using BCrypt.
   *
   * @param rawPassword the plaintext password to hash
   * @return BCrypt hash of the password
   */
  public static String bcrypt(String rawPassword) {
    Objects.requireNonNull(rawPassword, "rawPassword must not be null");
    return BCRYPT.encode(rawPassword);
  }

  /**
   * Verifies a password against a previously generated BCrypt hash.
   *
   * @param rawPassword the candidate password to verify
   * @param hashedPassword the stored BCrypt hash
   * @return {@code true} if the raw password matches the hash, otherwise {@code false}
   */
  public static boolean matchesBcrypt(String rawPassword, String hashedPassword) {
    Objects.requireNonNull(rawPassword, "rawPassword must not be null");
    Objects.requireNonNull(hashedPassword, "hashedPassword must not be null");
    return BCRYPT.matches(rawPassword, hashedPassword);
  }
}