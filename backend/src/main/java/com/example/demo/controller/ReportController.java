package com.example.demo.controller;

import com.example.demo.dto.ReportRequest;
import com.example.demo.service.GeoService;
import com.example.demo.service.JwtService;
import com.example.demo.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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


    @PostMapping
    public ResponseEntity<?> report(
            HttpServletRequest request, // JWT 토큰 헤더로 전달
            @RequestBody ReportRequest reportRequest) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String reporterEmail = jwtService.getEmailFromJWT(jwtToken);

            // 신고자의 위치 정보
            double latitude = reportRequest.getLatitude();
            double longitude = reportRequest.getLongitude();

            // 신고자의 위치 저장
            geoService.saveUserLocation(reporterEmail, longitude, latitude);

            // 반경 5km 내 사용자 검색
            List<String> nearbyUsers = geoService.findNearbyUsers(longitude, latitude, 5);

            // 주변 사용자들에게 알림 전송 (신고자 제외)
            notificationService.sendNotifications(reporterEmail, nearbyUsers, "Emergency alert from nearby!");

            return ResponseEntity.ok("Alert sent to nearby users.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        }
    }

}



