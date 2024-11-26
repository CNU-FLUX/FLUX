package com.example.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoMemberResponse {
    private String email;
    private String nickname;
    private String profileImage;
    private String jwtToken;
}
