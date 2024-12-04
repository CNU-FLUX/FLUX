package com.example.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private String type;
    private double latitude;
    private double longitude;
    private String timestamp;
    private String message;
    private String text; // optional
}
