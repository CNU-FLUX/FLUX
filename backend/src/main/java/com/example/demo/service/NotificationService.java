package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송
    private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:"; // Redis
    private final RedisTemplate<String, String> redisTemplate; // Redis 연동

    // WebSocket으로 알림 전송 및 저장
    public void sendNotifications(String reporterEmail, List<String> recipients, String message) {
        for (String email : recipients) {
            // 신고자(reporterEmail)와 수신자(email)가 동일하면 알림 제외
            if (email.equals(reporterEmail)) {
                System.out.println("[알림 제외] 신고자 이메일: " + email);
                continue;
            }

            // 알림 WebSocket 경로 생성
            String sanitizedEmail = email.replace("@", "_at_").replace(".", "_dot_");
            String destination = "/topic/" + sanitizedEmail;

            // 경로와 메시지를 로그로 출력
            System.out.println("[알림 전송] 경로: " + destination + ", 메시지: " + message);

            // WebSocket 메시지 전송
            messagingTemplate.convertAndSend(destination, message);

            // Redis에 알림 저장
            saveNotification(email, message);
        }
    }

    // Redis에 알림 저장
    private void saveNotification(String email, String message) {
        String notificationKey = NOTIFICATIONS_KEY_PREFIX + email;

        // 알림 데이터를 Redis List에 추가
        redisTemplate.opsForList().leftPush(notificationKey, message);

        // 알림 TTL 설정 (선택 사항 - 예: 1일 동안 유지)
        redisTemplate.expire(notificationKey, 1, TimeUnit.DAYS);
    }

    // 사용자 알림 조회
    public List<String> getUserNotifications(String email) {
        String notificationKey = NOTIFICATIONS_KEY_PREFIX + email;
        return redisTemplate.opsForList().range(notificationKey, 0, -1); // 전체 알림 조회
    }
}
