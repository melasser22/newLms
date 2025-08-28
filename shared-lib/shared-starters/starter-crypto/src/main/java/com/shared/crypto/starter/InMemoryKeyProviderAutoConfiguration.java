package com.shared.crypto.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
public class InMemoryKeyProviderAutoConfiguration {

    /**
     * Single-key model (backed by shared.crypto.in-memory):
     * - Decode ALL keys in props.inMemory.keys (kid -> base64) into one map.
     * - Active key is props.inMemory.activeKid.
     * - Consumers always resolve the *current* key (rotation = swap activeKid).
     */
    @Bean
    @ConditionalOnMissingBean
    public KeyProvider keyProvider(CryptoProperties props) {
        if (props.getInMemory() == null) {
            throw new IllegalStateException("Missing in-memory crypto configuration (shared.crypto.in-memory).");
        }

        var store = props.getInMemory();
        String activeKid = store.getActiveKid();
        Map<String, String> raw = store.getKeys() != null ? store.getKeys() : Map.of();

        if (raw.isEmpty()) {
            throw new IllegalStateException("No keys configured under shared.crypto.in-memory.keys");
        }

        Map<String, SecretKey> keys = new HashMap<>(raw.size());
        for (Map.Entry<String, String> e : raw.entrySet()) {
            byte[] material = Base64.getDecoder().decode(e.getValue());
            // Use "AES" so the same material works for AES/GCM; it's also acceptable for HMAC Mac.init.
            keys.put(e.getKey(), new SecretKeySpec(material, "AES"));
        }

        if (activeKid == null || activeKid.isBlank() || !keys.containsKey(activeKid)) {
            throw new IllegalStateException("Invalid shared.crypto.in-memory.activeKid: " + activeKid);
        }

        return new InMemoryKeyProvider(activeKid, Map.copyOf(keys));
    }

    @Bean
    @ConditionalOnMissingBean
    public HmacSigner hmacSigner(CryptoProperties props, KeyProvider keyProvider) {
        // Resolver ignores the provided kid and always uses the *current* key
        Function<String, Key> resolver = ignored -> keyProvider.getCurrentKey();
        String algo = (props.getHmac() != null && props.getHmac().getAlgorithm() != null)
                ? props.getHmac().getAlgorithm()
                : "HmacSHA256";
        return new HmacSigner(algo, resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public AesGcmCipher aesGcmCipher(CryptoProperties props, KeyProvider keyProvider) {
        int ivLen = (props.getAes() != null) ? props.getAes().getIvLength() : 12;
        int tagLen = (props.getAes() != null) ? props.getAes().getGcmTagLength() : 128;

        // Resolver ignores the provided kid and always uses the *current* key
        Function<String, SecretKey> resolver = ignored -> keyProvider.getCurrentKey();
        return new AesGcmCipher(ivLen, tagLen, resolver);
    }

    /* --- Single-key provider (starter-local) --- */

    public interface KeyProvider {
        String currentKeyId();
        SecretKey getKey(String kid);
        default SecretKey getCurrentKey() { return getKey(currentKeyId()); }
    }

    static final class InMemoryKeyProvider implements KeyProvider {
        private final String activeKid;
        private final Map<String, SecretKey> keys;

        InMemoryKeyProvider(String activeKid, Map<String, SecretKey> keys) {
            this.activeKid = activeKid;
            this.keys = keys;
        }
        public String currentKeyId() { return activeKid; }
        public SecretKey getKey(String kid) {
            SecretKey k = keys.get(kid);
            if (k == null) throw new IllegalArgumentException("Key not found for kid=" + kid);
            return k;
        }
    }

    /** Minimal HMAC service (sign/verify). */
    public static final class HmacSigner {
        private final String algorithm;
        private final Function<String, Key> resolver;

        public HmacSigner(String algorithm, Function<String, Key> resolver) {
            this.algorithm = algorithm; this.resolver = resolver;
        }

        public byte[] sign(String kid, byte[] data) {
            try {
                var mac = javax.crypto.Mac.getInstance(algorithm);
                mac.init(resolver.apply(kid)); // kid ignored by resolver in single-key model
                return mac.doFinal(data);
            } catch (Exception e) {
                throw new IllegalStateException("HMAC error", e);
            }
        }

        public boolean verify(String kid, byte[] data, byte[] signature) {
            return java.util.Arrays.equals(sign(kid, data), signature);
        }

        public String signBase64(String kid, String dataUtf8) {
            return Base64.getEncoder()
                         .encodeToString(sign(kid, dataUtf8.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /** Minimal AES-GCM cipher (random IV per message). */
    public static final class AesGcmCipher {
        private final int ivLength;
        private final int tagLengthBits;
        private final Function<String, SecretKey> resolver;

        public AesGcmCipher(int ivLength, int tagLengthBits, Function<String, SecretKey> resolver) {
            this.ivLength = ivLength; this.tagLengthBits = tagLengthBits; this.resolver = resolver;
        }

        public byte[] encrypt(String kid, byte[] plaintext, byte[] aad) {
            try {
                var iv = new byte[ivLength];
                new java.security.SecureRandom().nextBytes(iv);
                var spec = new javax.crypto.spec.GCMParameterSpec(tagLengthBits, iv);
                var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, resolver.apply(kid), spec);
                if (aad != null) cipher.updateAAD(aad);
                var ct = cipher.doFinal(plaintext);
                var out = new byte[iv.length + ct.length]; // iv || ct
                System.arraycopy(iv, 0, out, 0, iv.length);
                System.arraycopy(ct, 0, out, iv.length, ct.length);
                return out;
            } catch (Exception e) {
                throw new IllegalStateException("AES-GCM encrypt error", e);
            }
        }

        public byte[] decrypt(String kid, byte[] ivAndCiphertext, byte[] aad) {
            try {
                var iv = java.util.Arrays.copyOfRange(ivAndCiphertext, 0, ivLength);
                var ct = java.util.Arrays.copyOfRange(ivAndCiphertext, ivLength, ivAndCiphertext.length);
                var spec = new javax.crypto.spec.GCMParameterSpec(tagLengthBits, iv);
                var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, resolver.apply(kid), spec);
                if (aad != null) cipher.updateAAD(aad);
                return cipher.doFinal(ct);
            } catch (Exception e) {
                throw new IllegalStateException("AES-GCM decrypt error", e);
            }
        }
    }
}
