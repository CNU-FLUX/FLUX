package com.example.demo.service;

import com.example.demo.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송
    private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:"; // Redis

    private final RedisTemplate<String, Object> redisTemplate;

    private final BlockchainService blockchainService;

    // WebSocket으로 알림 전송 및 저장
    public void sendNotifications(String reporterEmail, List<String> recipients, String transactionHash, Long reportId, Date timestamp) {
        // 블록체인 메시지 조회
        System.out.println("[DEBUG] 블록체인 메시지 조회 시작");
        String message = blockchainService.getMessageFromBlockchain(transactionHash);
        System.out.println("[DEBUG] 블록체인에서 조회한 메시지: " + message);


        for (String recipientEmail : recipients) {
            // 신고자(reporterEmail)와 수신자(recipientEmail)가 동일하면 알림 제외
            if (recipientEmail.equals(reporterEmail)) {
                System.out.println("[알림 제외] 신고자 이메일: " + recipientEmail);
                continue;
            }

            // 알림 WebSocket 경로 생성
            String sanitizedEmail = recipientEmail.replace("@", "_at_").replace(".", "_dot_");
            String destination = "/topic/" + sanitizedEmail;

            // 경로와 메시지를 로그로 출력
            System.out.println("[알림 전송] 경로: " + destination + ", 메시지: " + message);

            // 알림 엔티티 생성
            Notification notification = Notification.builder()
                    .reportId(reportId)
                    .message(message)
                    .timestamp(timestamp)
                    .senderEmail(reporterEmail) // 신고자 이메일 추가
                    .build();

            // WebSocket 메시지 전송
            try {
                messagingTemplate.convertAndSend(destination, notification);
                System.out.println("[DEBUG] WebSocket 메시지 전송 성공: " + notification);
            } catch (Exception e) {
                System.err.println("[ERROR] WebSocket 메시지 전송 실패: " + e.getMessage());
            }
            // Redis에 알림 저장
            saveNotification(recipientEmail, notification);
        }
    }

    // Redis에 알림 저장
    private void saveNotification(String recipient, Notification notification) {
        try {
            String notificationKey = NOTIFICATIONS_KEY_PREFIX + recipient;
            redisTemplate.opsForList().leftPush(notificationKey, notification);
            System.out.println("[DEBUG] 알림 저장 성공: " + notification);
        } catch (Exception e) {
            System.err.println("[ERROR] 알림 저장 실패: " + e.getMessage());
        }
    }

    // 받은 알림 조회
    public List<Notification> getUserNotifications(String email) {
        String notificationKey = "notifications:" + email;
        List<Object> notificationsFromRedis = redisTemplate.opsForList().range(notificationKey, 0, -1);

        // Redis에서 객체를 Notification 타입으로 변환
        List<Notification> notifications = new ArrayList<>();
        if (notificationsFromRedis != null) {
            for (Object notification : notificationsFromRedis) {
                notifications.add((Notification) notification);
            }
        }
        return notifications;
    }
}
