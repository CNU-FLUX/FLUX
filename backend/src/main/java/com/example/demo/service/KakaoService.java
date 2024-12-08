package com.example.demo.service;

import com.example.demo.dto.KakaoMemberResponse;
import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.util.*;
import org.springframework.http.*;

import java.time.OffsetDateTime;

@Getter
@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${kakao.api_key}")
    private String kakaoApiKey;

    @Value("${kakao.redirect_uri}")
    private String kakaoRedirectUri;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GeoService geoService;

    private final RedisTemplate<String, String> redisTemplate; // Redis 템플릿 추가


    // 카카오 로그인 URL 생성
    public String getKakaoLoginUrl() {
        return "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=" +
                kakaoApiKey + "&redirect_uri=" + kakaoRedirectUri;
    }

    // 카카오 로그인 처리
    public KakaoMemberResponse handleKakaoLogin(String code, Double latitude, Double longitude) throws Exception {
        // Access Token 발급
        String accessToken = getAccessToken(code);

        // 사용자 정보 가져오기 및 저장
        return getUserInfo(accessToken, latitude, longitude);
    }

    //인가 코드를 받아서 accessToken을 반환
    private String getAccessToken(String code) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String reqUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoApiKey);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        // 디버깅 로그 추가
        System.out.println("Kakao Token API Response: " + response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseBody = objectMapper.readTree(response.getBody());

        return responseBody.path("access_token").asText();
    }

    // 액세스 토큰을 사용해 프로필 정보를 가져오고 User 엔티티에 저장
    // 사용자 정보 조회 및 JWT 생성
    public KakaoMemberResponse getUserInfo(String accessToken, Double latitude, Double longitude) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String reqUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.GET, request, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseBody = objectMapper.readTree(response.getBody());

        Long kakaoId = responseBody.path("id").asLong();
        String email = responseBody.path("kakao_account").path("email").asText();
        String nickname = responseBody.path("kakao_account").path("profile").path("nickname").asText();
        String profileImage = responseBody.path("kakao_account").path("profile").path("profile_image_url").asText();

        // Redis에서 accountId 조회
        String accountIdKey = "acc_id:" + email;
        String accountId = redisTemplate.opsForValue().get(accountIdKey);
        System.out.println("[DEBUG] Redis에서 조회한 accountId: " + accountId);

        // 사용자 정보 저장 또는 업데이트
        Member member = memberRepository.findById(email)
                .map(existingMember -> {
                    // 사용자 정보 업데이트
                    existingMember.setNickname(nickname);
                    existingMember.setKakaoId(kakaoId);
                    existingMember.setProfileImage(profileImage);
                    existingMember.setAccessToken(accessToken);
                    existingMember.setAccountId(accountId);
                    existingMember.setUpdatedAt(OffsetDateTime.now());
                    return existingMember;
                })
                .orElse(Member.builder()
                        // 새로운 사용자 생성
                        .email(email)
                        .nickname(nickname)
                        .kakaoId(kakaoId)
                        .profileImage(profileImage)
                        .accessToken(accessToken)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .pushEnabled(true) // 기본값 설정
                        .accountId(accountId)
                        .build());

        // Redis에 저장 (새로 생성된 사용자 또는 업데이트된 사용자)
        memberRepository.save(member);

        // Redis에 사용자 위치 정보 저장
        if (latitude != null && longitude != null) {
            geoService.saveUserLocation(email, longitude, latitude);
        } else {
            // 기본 위치 저장 (임의로 설정, 예: 대전 충남대학교)
            geoService.saveUserLocation(member.getEmail(), 127.3445, 36.3659);
        }

        // JWT 발급
        String jwtToken = jwtService.createJWT(email);

        return KakaoMemberResponse.builder()
                .email(email)
                .nickname(nickname)
                .profileImage(profileImage)
                .jwtToken(jwtToken)
                .build();
    }


    //accessToken을 받아서 로그아웃 시키는 메서드
    public boolean kakaoLogout(String accessToken) {
        // JWT 토큰 검증 및 이메일 추출
        RestTemplate restTemplate = new RestTemplate();
        String reqUrl = "https://kapi.kakao.com/v1/user/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken); // 액세스 토큰 추가
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); // Content-Type 추가

        // HttpEntity 생성 (headers만 포함)
        HttpEntity<Void> kakaoLogoutRequest = new HttpEntity<>(headers);

        try {
            // POST 방식으로 요청
            ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, kakaoLogoutRequest, String.class);

            // 응답 상태 코드 확인 (200 OK)
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Logout success: " + response.getBody());
                return true; // 로그아웃 성공
            } else {
                System.out.println("Logout failed: " + response.getBody());
                return false; // 로그아웃 실패
            }

        } catch (HttpClientErrorException e) {
            System.out.println("Error during logout: " + e.getStatusCode());
            System.out.println("Response Body: " + e.getResponseBodyAsString());
            return false; // 예외 처리 중 실패 반환
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 예외 처리 중 실패 반환
        }

    }
}
