package com.example.demo.controller;

import com.example.demo.entity.Notification;
import com.example.demo.service.JwtService;
import com.example.demo.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final JwtService jwtService;
    private final NotificationService notificationService;

    // 사용자 알림 조회 API
    @GetMapping("/my")
    public ResponseEntity<?> getUserNotifications(HttpServletRequest request) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            // 알림 조회
            List<Notification> notifications = notificationService.getUserNotifications(email);

            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch notifications: " + e.getMessage());
        }
    }
}
