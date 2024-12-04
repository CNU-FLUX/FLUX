package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String type;     // 신고 타입
    private double latitude;    // 위도
    private double longitude;   // 경도
    private String timestamp;   // 신고 시간
    private String text; // 기타 신고 세부 내용 (optional)
}
