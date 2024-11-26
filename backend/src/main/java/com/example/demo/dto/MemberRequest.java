package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequest {
    private String email;
    private String nickname;
    private Double latitude;  // 위도 (Optional)
    private Double longitude; // 경도 (Optional)
}
