package com.example.demo.service;

import com.example.demo.dto.ReportRequest;
import com.example.demo.dto.ReportResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RedisTemplate<String, String> redisTemplate;

    // 신고 데이터 저장
    public void saveReport(String email, ReportRequest reportRequest, String alertMessage) {
        // 신고 ID 생성 (유저별 증가)
        String reportIdKey = "user_report_id:" + email; // 유저별 신고 ID 관리 키
        Long reportId = redisTemplate.opsForValue().increment(reportIdKey); // 신고 ID 증가

        // 신고 데이터를 JSON 문자열로 변환
        String reportJson = convertReportToJson(reportId, reportRequest, alertMessage);

        // Redis에 신고 데이터 추가 (List 구조 사용)
        redisTemplate.opsForList().leftPush("user_reports:" + email, reportJson);
    }

    // JSON 변환 로직
    private String convertReportToJson(Long reportId, ReportRequest reportRequest, String alertMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode reportNode = objectMapper.createObjectNode();

        reportNode.put("id", reportId);
        reportNode.put("type", reportRequest.getType());
        reportNode.put("latitude", reportRequest.getLatitude());
        reportNode.put("longitude", reportRequest.getLongitude());
        reportNode.put("timestamp", reportRequest.getTimestamp());
        reportNode.put("message", alertMessage); // 타입별 메시지 포함
        if ("other".equalsIgnoreCase(reportRequest.getType())) {
            reportNode.put("text", reportRequest.getText());
        }

        try {
            return objectMapper.writeValueAsString(reportNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert report to JSON", e);
        }
    }

    public List<ReportResponse> getUserReports(String email) {
        List<String> reportsJson = redisTemplate.opsForList().range("user_reports:" + email, 0, -1);

        ObjectMapper objectMapper = new ObjectMapper();
        List<ReportResponse> reports = new ArrayList<>();

        for (String reportJson : reportsJson) {
            try {
                reports.add(objectMapper.readValue(reportJson, ReportResponse.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return reports;
    }
}
