package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // WebSocket으로 알림 전송
    public void sendNotifications(String reporterEmail, List<String> recipients, String message) {
        for (String email : recipients) {
            // 신고자(reporterEmail)와 수신자(email)가 동일하면 알림 제외
            if (email.equals(reporterEmail)) {
                System.out.println("[알림 제외] 신고자 이메일: " + email);
                continue;
            }

            // 알림 경로 생성
            String sanitizedEmail = email.replace("@", "_at_").replace(".", "_dot_");
            String destination = "/topic/" + sanitizedEmail;

            // 경로와 메시지를 로그로 출력
            System.out.println("[알림 전송] 경로: " + destination + ", 메시지: " + message);

            // 메시지 전송
            messagingTemplate.convertAndSend(destination, message);
        }
    }

}
