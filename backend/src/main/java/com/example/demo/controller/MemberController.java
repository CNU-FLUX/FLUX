package com.example.demo.controller;

import com.example.demo.dto.MemberRequest;
import com.example.demo.entity.Member;
import com.example.demo.service.BlockchainService;
import com.example.demo.service.JwtService;
import com.example.demo.service.MemberService;
import com.example.demo.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final JwtService jwtService;
    private final BlockchainService blockchainService;
    private final RedisService redisService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberRequest memberRequest) {
        try {
            System.out.println("회원가입 요청 데이터: "+memberRequest);
            Member newMember = memberService.signupMember(memberRequest);
            return ResponseEntity.ok(newMember);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("이미 존재하는 이메일입니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 내부 오류가 발생했습니다.");
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody MemberRequest memberRequest) {
        try {
            String jwtToken = memberService.loginMember(memberRequest);
            return ResponseEntity.ok().header("Authorization", "Bearer " + jwtToken).body("로그인 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("Invalid email or password");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Login failed");
        }
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/my")
    public ResponseEntity<Member> getMemberInfo(HttpServletRequest request) {
        try {
            String token = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(token);

            Member member = memberService.findMemberByEmail(email);

            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    /**
     * JWT 검증 및 사용자 ID 추출
     */
    @GetMapping("/validate-token")
    public ResponseEntity<String> validateToken(HttpServletRequest request) {
        try {
            // JWT에서 사용자 ID 추출
            String token = jwtService.extractTokenFromRequest(request);
            token = token.replace("Bearer ", "");
            jwtService.validateJWT(token);

            String email = jwtService.getEmailFromJWT(token);
            return ResponseEntity.ok("Authenticated user email: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired token: " + e.getMessage());
        }
    }

    // Push 알림 설정 업데이트
    @PostMapping("/settings/push-notification")
    public ResponseEntity<?> updatePushNotificationSetting(HttpServletRequest request,
                                                           @RequestParam boolean pushEnabled) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            // Push 알림 설정 업데이트
            memberService.updatePushNotificationSetting(email, pushEnabled);

            return ResponseEntity.ok("Push notification setting updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update push notification setting: " + e.getMessage());
        }
    }

    @GetMapping("/get-blockToken")
    public ResponseEntity<?> getMemberTokenBalance(HttpServletRequest request) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            // RedisService를 통해 이메일 기반으로 블록체인 계정 조회
            Object accountIdObj = redisService.getValue("acc_id:" + email);

            if (accountIdObj == null || !(accountIdObj instanceof String)) {
                return ResponseEntity.badRequest().body("Account ID not found or invalid for the given email");
            }

            String accountId = (String) accountIdObj;

            // BlockchainService를 통해 블록체인 계정의 잔액 조회
            String balance = blockchainService.getTokenBalance(accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "email", email,
                            "accountId", accountId,
                            "balance", balance
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to retrieve token balance: " + e.getMessage());
        }
    }
}
