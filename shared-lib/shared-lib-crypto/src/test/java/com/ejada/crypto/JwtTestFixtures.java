package com.ejada.crypto;

import javax.crypto.SecretKey;

final class JwtTestFixtures {

    static final String TEST_SECRET_B64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private JwtTestFixtures() {
    }

    static SecretKey signingKey() {
        return JwtTokenService.createKey(TEST_SECRET_B64);
    }
}
