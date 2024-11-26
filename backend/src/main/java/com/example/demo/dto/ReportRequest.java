package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private double latitude;    // 신고자의 위도
    private double longitude;   // 신고자의 경도
}
