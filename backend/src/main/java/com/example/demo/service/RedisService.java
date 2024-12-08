package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Redis에서 값을 조회하고 JSON 또는 단순 문자열로 처리
     *
     * @param key Redis 키
     * @return JSON 객체 또는 문자열 값
     */
    public Object getValue(String key) {
        String value = (String) redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null; // 키가 없는 경우
        }

        if (isJsonString(value)) {
            // JSON 형태인 경우 역직렬화
            return deserializeJson(value, Object.class);
        }

        // 단순 문자열인 경우 그대로 반환
        return value;
    }

    /**
     * JSON 문자열 여부 확인
     *
     * @param value 검사할 문자열
     * @return JSON 형식 여부
     */
    private boolean isJsonString(String value) {
        try {
            objectMapper.readTree(value);
            return true; // JSON 형식임
        } catch (Exception e) {
            return false; // JSON 형식 아님
        }
    }

    /**
     * JSON 문자열을 특정 타입으로 역직렬화
     *
     * @param json      JSON 문자열
     * @param valueType 변환할 클래스 타입
     * @return 역직렬화된 객체
     */
    private <T> T deserializeJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON: " + e.getMessage(), e);
        }
    }
}
