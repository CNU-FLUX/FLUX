package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.OffsetDateTime;

@RedisHash("member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    private String email; // Redis의 Key 역할이자 사용자 식별자

    private String nickname; // 사용자 닉네임

    private Long kakaoId; // 카카오 사용자 ID

    private String profileImage; // 프로필 이미지 URL

    private String accessToken; // 최신 액세스 토큰

    private OffsetDateTime createdAt; // 생성 시간

    private OffsetDateTime updatedAt; // 업데이트 시간

//    private String accountId; // 블록체인 연동 Id

    private boolean pushEnabled; // Push 알림 허용 여부


    public Member(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
        this.pushEnabled = true;
    }

}
