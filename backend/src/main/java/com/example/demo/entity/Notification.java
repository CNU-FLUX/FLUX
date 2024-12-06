package com.example.demo.entity;

import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private String senderEmail;  // 발신자 이메일 (신고자)
    private Long reportId;       // 그 신고자의 신고 ID
    private String message;      // 알림 메시지
    private Date timestamp; // 알림 시간
}