package com.example.demo.controller;

import com.example.demo.dto.ReportRequest;
import com.example.demo.entity.Report;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
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
            System.out.println("[DEBUG] JWT에서 추출한 이메일: " + reporterEmail);

            // 신고자의 위치 정보
            double latitude = reportRequest.getLatitude();
            double longitude = reportRequest.getLongitude();

            // 신고자의 위치 저장
            System.out.println("[DEBUG] 신고자의 위치 정보 저장 시작");
            geoService.saveUserLocation(reporterEmail, longitude, latitude);

            // 타임스탬프 검증
            if (reportRequest.getTimestamp() == null) {
                System.out.println("[DEBUG] Timestamp 누락 확인");
                return ResponseEntity.badRequest().body("Timestamp is required.");
            }

            // 신고 타입별 메시지 생성
            System.out.println("[DEBUG] Alert 메시지 생성 시작");
            String alertMessage = generateAlertMessage(reportRequest);
            System.out.println("[DEBUG] Alert 메시지 생성 완료: " + alertMessage);

            // 블록체인에 신고 메시지 전송, 해시값 얻음


            // 신고 데이터 저장
            System.out.println("[DEBUG] 신고 데이터 저장 시작");
            Long reportId = reportService.saveReport(reporterEmail, reportRequest, alertMessage);
            System.out.println("[DEBUG] 신고 데이터 저장 완료, Report ID: " + reportId);

            // 반경 5km 내 사용자 검색
            System.out.println("[DEBUG] 반경 5km 내 사용자 검색 시작");
            List<String> nearbyUsers = geoService.findNearbyUsers(longitude, latitude, 5);
            System.out.println("[DEBUG] 반경 5km 내 사용자 검색 완료: " + nearbyUsers);

            // Push 알림이 활성화된 사용자 필터링
            System.out.println("[DEBUG] Push 알림 가능 사용자 필터링 시작");
            List<String> pushEnabledUsers = memberService.filterPushEnabledUsers(nearbyUsers);
            System.out.println("[DEBUG] Push 알림 가능 사용자: " + pushEnabledUsers);


            // 이 얻은 해시값으로 블록체인에서 메시지 얻기, 이 메시지로 알림 전송


            System.out.println("[DEBUG] sendNotifications 메서드 호출 시작");
            // 주변 사용자들에게 알림 전송 (신고자 제외)
            // 누가, 언제, 어떤 메시지를 보내는지?
            notificationService.sendNotifications(reporterEmail, pushEnabledUsers, alertMessage, reportId, reportRequest.getTimestamp());
            System.out.println("[DEBUG] sendNotifications 메서드 호출 완료");

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

    @GetMapping("/my")
    public ResponseEntity<?> getUserReports(HttpServletRequest request) {
        try {
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            // Report 엔티티 리스트 조회
            List<Report> reportEntities = reportService.getUserReports(email);

            return ResponseEntity.ok(reportEntities);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch reports: " + e.getMessage());
        }
    }


}



