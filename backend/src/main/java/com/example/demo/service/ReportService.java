package com.example.demo.service;

import com.example.demo.dto.ReportRequest;
import com.example.demo.entity.Report;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> jsonRedisTemplate; // JSON 전용 템플릿 추가

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
        jsonRedisTemplate.opsForList().leftPush(reportListKey, report);

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
                if (report instanceof Report) {
                    reports.add((Report) report); // 직렬화된 객체
                } else if (report instanceof String) {
                    // JSON 문자열로 저장된 경우 역직렬화
                    try {
                        reports.add((Report) jsonRedisTemplate.getValueSerializer().deserialize(
                                ((String) report).getBytes()
                        ));
                    } catch (Exception e) {
                        System.err.println("[ERROR] 신고 데이터 역직렬화 실패: " + e.getMessage());
                    }
                }
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
                List<Object> reports = jsonRedisTemplate.opsForList().range(key, 0, -1);
                if (reports != null) {
                    for (Object report : reports) {
                        if (report instanceof String) {
                            try {
                                // JSON 문자열을 Report 객체로 변환
                                Report reportObject = new ObjectMapper().readValue((String) report, Report.class);
                                allReports.add(reportObject);
                            } catch (Exception e) {
                                System.err.println("[ERROR] JSON 변환 실패: " + e.getMessage());
                            }
                        } else if (report instanceof Report) {
                            // 이미 객체인 경우 바로 추가
                            allReports.add((Report) report);
                        } else {
                            System.err.println("[WARN] 알 수 없는 데이터 형식: " + report.getClass());
                        }
                    }
                }
            }
        }
        return allReports;
    }
}
