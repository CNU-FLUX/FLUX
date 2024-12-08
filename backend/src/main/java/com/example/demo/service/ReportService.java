package com.example.demo.service;

import com.example.demo.dto.ReportRequest;
import com.example.demo.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REPORTS_KEY_PREFIX = "user_reports:";


    // 신고 데이터 저장
    public Long saveReport(String email, ReportRequest reportRequest, String alertMessage) {
        // 신고 ID 생성 (유저별 증가)
        String reportIdKey = "user_report_id:" + email; // 유저별 신고 ID 관리 키
        Long reportId = redisTemplate.opsForValue().increment(reportIdKey); // 신고 ID 증가

        // 신고 엔티티 생성
        Report report = Report.builder()
                .id(reportId)
                .type(reportRequest.getType())
                .latitude(reportRequest.getLatitude())
                .longitude(reportRequest.getLongitude())
                .timestamp(reportRequest.getTimestamp())
                .message(alertMessage)
                .text(reportRequest.getText())
                .build();

        // Redis에 신고 데이터 추가 (List 구조 사용)
        String reportListKey = "user_reports:" + email;
        redisTemplate.opsForList().leftPush(reportListKey, report);

        return reportId;
    }


    // 신고 데이터 조회
    public List<Report> getUserReports(String email) {
        String reportListKey = REPORTS_KEY_PREFIX + email;
        List<Object> reportsFromRedis = redisTemplate.opsForList().range(reportListKey, 0, -1);

        // Redis에서 객체를 Report 타입으로 변환
        List<Report> reports = new ArrayList<>();
        if (reportsFromRedis != null) {
            for (Object report : reportsFromRedis) {
                reports.add((Report) report);
            }
        }
        return reports;
    }

    // 모든 리포트를 조회
    public List<Report> getAllReports() {
        List<Report> allReports = new ArrayList<>();

        // Redis의 모든 키 조회 (user_reports:* 형식)
        Set<String> keys = redisTemplate.keys(REPORTS_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                List<Object> reports = redisTemplate.opsForList().range(key, 0, -1);
                if (reports != null) {
                    for (Object report : reports) {
                        allReports.add((Report) report);
                    }
                }
            }
        }
        return allReports;
    }
}
