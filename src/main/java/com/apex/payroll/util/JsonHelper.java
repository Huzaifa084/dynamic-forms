package com.apex.payroll.util;

import com.apex.payroll.exception.JsonConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonHelper {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonHelper() {
    }

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSON conversion failed", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new JsonConversionException("JSON parsing failed", e);
        }
    }
}
