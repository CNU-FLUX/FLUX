package com.example.demo.controller;

import com.example.demo.entity.Member;
import com.example.demo.dto.KakaoMemberResponse;
import com.example.demo.service.JwtService;
import com.example.demo.service.KakaoService;
import com.example.demo.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class KakaoController {

    private final KakaoService kakaoService;
    private final JwtService jwtService;
    private final MemberService memberService;

    // 프론트엔드에서 로그인 URL을 요청할 때 호출되는 엔드포인트
    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(loginUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code,
                                        @RequestParam(value = "latitude", required = false) Double latitude,
                                        @RequestParam(value = "longitude", required = false) Double longitude) {
        try {
            KakaoMemberResponse memberResponse = kakaoService.handleKakaoLogin(code, latitude, longitude);
            // JWT 및 사용자 정보를 클라이언트에 전달
            return ResponseEntity.ok(memberResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Login failed");
        }
    }

    // 로그아웃 처리
    @PostMapping("/logout")
    public ResponseEntity<String> kakaoLogout(@RequestHeader("Authorization") String jwtToken) {
        try {
            // JWT 토큰 검증 및 이메일 추출
            String email = jwtService.getEmailFromJWT(jwtToken);

            // 이메일로 사용자 조회 및 카카오 AccessToken 추출
            Member member = memberService.findMemberByEmail(email);
            String accessToken = member.getAccessToken();

            // 카카오 로그아웃 처리
            boolean isLoggedOut = kakaoService.kakaoLogout(accessToken);

            if (isLoggedOut) {
                return ResponseEntity.ok("Successfully logged out");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to log out from Kakao");
            }

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body("Error during logout: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }

}
