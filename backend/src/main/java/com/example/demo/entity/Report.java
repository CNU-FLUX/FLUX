package com.example.demo.entity;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    private Long id;             // 신고 ID
    private String type;         // 신고 유형
    private double latitude;     // 위도
    private double longitude;    // 경도
    private Date timestamp; // 신고 시간
    private String message;      // 신고 메시지
    private String text;         // 기타 유형의 신고 세부 내용 (optional)
    private boolean trust;       // 신뢰 여부
}