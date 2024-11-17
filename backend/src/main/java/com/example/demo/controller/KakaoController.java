package com.example.demo.controller;

import com.example.demo.entity.Member;
import com.example.demo.entity.KakaoMemberResponse;
import com.example.demo.service.JwtService;
import com.example.demo.service.KakaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class KakaoController {

    private final KakaoService kakaoService;
    private final JwtService jwtService;

    // 프론트엔드에서 로그인 URL을 요청할 때 호출되는 엔드포인트
    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(loginUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code) {
        try {
            KakaoMemberResponse memberResponse = kakaoService.handleKakaoLogin(code);
            // JWT 및 사용자 정보를 클라이언트에 전달
            return ResponseEntity.ok(memberResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null); // 에러 발생 시 500 반환
        }
    }

    // JWT 검증 및 사용자 ID 추출
    @GetMapping("/validate-token")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String token) {
        try {
            // "Bearer " 제거 후 토큰 검증
            token = token.replace("Bearer ", "");
            jwtService.validateJWT(token);

            // 토큰에서 사용자 ID 추출
            String memberId = jwtService.getMemberId();
            return ResponseEntity.ok("Authenticated user ID: " + memberId);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired token: " + e.getMessage());
        }
    }


    // 로그아웃 처리
    @PostMapping("/logout")
    public ResponseEntity<String> kakaoLogout(@RequestParam("accessToken") String accessToken) {
        try {
            String token = accessToken.replace("Bearer ", "");
            kakaoService.kakaoLogout(token);
            return ResponseEntity.ok("Successfully logged out");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

}
