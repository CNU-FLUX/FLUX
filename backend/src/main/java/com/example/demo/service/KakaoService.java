package com.example.demo.service;

import com.example.demo.entity.KakaoMemberResponse;
import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.util.*;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

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


    // 카카오 로그인 URL 생성
    public String getKakaoLoginUrl() {
        return "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=" +
                kakaoApiKey + "&redirect_uri=" + kakaoRedirectUri;
    }

    // 카카오 로그인 처리
    public KakaoMemberResponse handleKakaoLogin(String code) throws Exception {
        // Access Token 발급
        String accessToken = getAccessToken(code);

        // 사용자 정보 가져오기 및 저장
        return getUserInfo(accessToken);
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
    public KakaoMemberResponse getUserInfo(String accessToken) throws Exception {
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

        // 사용자 정보 저장 또는 업데이트
        Member member = memberRepository.findById(email)
                .map(existingMember -> {
                    // 사용자 정보 업데이트
                    existingMember.setNickname(nickname);
                    existingMember.setKakaoId(kakaoId);
                    existingMember.setProfileImage(profileImage);
                    existingMember.setAccessToken(accessToken);
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
                        .build());

        // Redis에 저장 (새로 생성된 사용자 또는 업데이트된 사용자)
        memberRepository.save(member);

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
    public void kakaoLogout(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();
        String reqUrl = "https://kapi.kakao.com/v1/user/logout";

        // HttpHeader 오브젝트
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        // HttpEntity 생성 (headers만 포함)
        HttpEntity<Void> kakaoLogoutRequest = new HttpEntity<>(headers);

        try {
            // POST 방식으로 요청
            ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, kakaoLogoutRequest, String.class);

            // 응답 코드 출력
            int responseCode = response.getStatusCodeValue();
            System.out.println("[KakaoApi.kakaoLogout] responseCode : " + responseCode);

            // 응답 본문 출력
            String result = response.getBody();
            System.out.println("kakao logout - responseBody = " + result);

        } catch (HttpClientErrorException e) {
            System.out.println("Error during logout: " + e.getStatusCode());
            System.out.println("Response Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
