package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)// DB에서 자동 증가
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 이메일 (중복 방지)

    private String nickname; // 사용자 닉네임

    @Column(unique = true)
    private Long kakaoId; // 카카오 사용자 ID (중복 방지)

    private String profileImage; // 프로필 이미지 URL

    private String accessToken; // 최신 액세스 토큰

    @Column(columnDefinition = "TIMESTAMP")
    private java.time.OffsetDateTime createdAt; // 생성 시간

    @Column(columnDefinition = "TIMESTAMP")
    private java.time.OffsetDateTime updatedAt; // 업데이트 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.OffsetDateTime.now();
    }
}

