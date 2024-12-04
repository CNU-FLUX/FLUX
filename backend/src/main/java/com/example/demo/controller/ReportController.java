package com.example.demo.controller;

import com.example.demo.dto.ReportRequest;
import com.example.demo.dto.ReportResponse;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final GeoService geoService;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final ReportService reportService;
    private final MemberService memberService;


    @PostMapping
    public ResponseEntity<?> report(
            HttpServletRequest request, // JWT 토큰 헤더로 전달
            @RequestBody ReportRequest reportRequest) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String reporterEmail = jwtService.getEmailFromJWT(jwtToken);

            // 신고 타입이 기타인 경우, 세부 내용 검증
            if ("other".equalsIgnoreCase(reportRequest.getType()) &&
                    (reportRequest.getText() == null || reportRequest.getText().isEmpty())) {
                return ResponseEntity.badRequest().body("Text is required for 'other' type reports.");
            }

            // 신고자의 위치 정보
            double latitude = reportRequest.getLatitude();
            double longitude = reportRequest.getLongitude();

            // 신고자의 위치 저장
            geoService.saveUserLocation(reporterEmail, longitude, latitude);

            // 신고 타입별 메시지 생성
            String alertMessage = generateAlertMessage(reportRequest);

            // 신고 데이터 저장
            reportService.saveReport(reporterEmail, reportRequest, alertMessage);


            // 반경 5km 내 사용자 검색
            List<String> nearbyUsers = geoService.findNearbyUsers(longitude, latitude, 5);

            // Push 알림이 활성화된 사용자 필터링
            List<String> pushEnabledUsers = memberService.filterPushEnabledUsers(nearbyUsers);


            // 주변 사용자들에게 알림 전송 (신고자 제외)
            notificationService.sendNotifications(reporterEmail, pushEnabledUsers, "Emergency alert from nearby!");


            return ResponseEntity.ok("Alert sent to nearby users.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        }
    }

    // 신고 메시지 생성 로직
    private String generateAlertMessage(ReportRequest reportRequest) {
        String type = reportRequest.getType();
        switch (type.toLowerCase()) {
            case "truck":
                return "트럭 낙하물이 신고되었습니다.";
            case "road":
                return "도로 위 장애물이 신고되었습니다.";
            case "car":
                return "역주행이 신고되었습니다.";
            case "alcohol":
                return "음주 또는 졸음운전이 신고되었습니다.";
            case "other":
                if (reportRequest.getText() == null || reportRequest.getText().isEmpty()) {
                    return "기타 신고가 접수되었습니다.";
                }
                return "기타 신고: " + reportRequest.getText();
            default:
                return "알 수 없는 유형의 신고가 접수되었습니다.";
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserReports(HttpServletRequest request) {
        try {
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            List<ReportResponse> reports = reportService.getUserReports(email);

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch reports: " + e.getMessage());
        }
    }


}



