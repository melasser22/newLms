package com.common.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.common.exception.JsonSerializationException;

/**
 * Utility for JSON serialization and deserialization.
 * Uses a single shared ObjectMapper (thread-safe).
 */
public final class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules() // register JavaTimeModule, JDK8, etc.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {
        // utility class
    }

    /**
     * Convert an object to JSON string.
     *
     * @param obj the object to serialize
     * @return JSON representation
     * @throws RuntimeException if serialization fails
     */
    public static String toJson(Object obj) throws JsonSerializationException {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // wrap and propagate checked exception
            throw new JsonSerializationException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object.
     *
     * @param json  JSON string
     * @param clazz target class
     * @return deserialized object
     * @throws RuntimeException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonSerializationException {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Convert JSON string to object using generic type reference.
     *
     * Example:
     * List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>()
     * {});
     */
    public static <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) throws JsonSerializationException {
        try {
            return mapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON to type " + typeRef.getType(), e);
        }
    }

    /**
     * Access to the underlying ObjectMapper for advanced use cases.
     */
    public static ObjectMapper mapper() {
        return mapper;
    }
}
