package com.example.demo.service;

import com.example.demo.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeoService {

    private static final String GEO_KEY = "user_locations";  // Redis에서 위치 데이터를 저장할 키
    private final RedisTemplate<String, String> redisTemplate; // String 타입으로 변경

    // 사용자 위치 저장
    public void saveUserLocation(String email, double longitude, double latitude) {
        // Redis에 사용자 위치 저장 (GEOADD)
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(longitude, latitude), email);
    }

    // 주변 사용자 검색 (반경 km)
    public List<String> findNearbyUsers(double longitude, double latitude, double radius) {
        // 반경 거리 설정
        // Circle 생성 (중심점과 반경)
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(radius, Metrics.KILOMETERS));

        // Redis에서 반경 내 사용자 검색
        return redisTemplate.opsForGeo()
                .radius(GEO_KEY, circle)
                .getContent()
                .stream()
                .map(result -> result.getContent().getName()) // GeoLocation의 이름(이메일) 가져오기
                .collect(Collectors.toList());
    }
}