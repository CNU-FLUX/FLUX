package com.example.demo.service;

import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import com.example.demo.dto.MemberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final JwtService jwtService;

    private final GeoService geoService;

    private final RedisTemplate<String, Object> redisTemplate;


    public Member signupMember(MemberRequest memberRequest) {
        if (memberRepository.findByEmail(memberRequest.getEmail()).isPresent()) {
            System.out.println("이미 존재하는 이메일: " + memberRequest.getEmail());
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Member member = new Member(memberRequest.getEmail(), memberRequest.getNickname());
        Member savedMember = memberRepository.save(member);
        System.out.println("회원가입 성공: " + savedMember);

        // 위치 정보가 포함되어 있는 경우 Redis에 저장, 그렇지 않으면 기본 위치 저장
        if (memberRequest.getLatitude() != null && memberRequest.getLongitude() != null) {
            geoService.saveUserLocation(
                    savedMember.getEmail(),
                    memberRequest.getLongitude(),
                    memberRequest.getLatitude()
            );
        } else {
            // 기본 위치 저장 (대전 충남대학교)
            geoService.saveUserLocation(
                    savedMember.getEmail(),
                    127.3445, // 경도
                    36.3659  // 위도
            );
        }

        return savedMember;
    }

    public String loginMember(MemberRequest memberRequest) {
        Member dbMember = memberRepository.findByEmail(memberRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 위치 정보가 포함되어 있는 경우 Redis에 저장, 그렇지 않으면 기본 위치 저장
        if (memberRequest.getLatitude() != null && memberRequest.getLongitude() != null) {
            geoService.saveUserLocation(
                    dbMember.getEmail(),
                    memberRequest.getLongitude(),
                    memberRequest.getLatitude()
            );
        } else {
            // 기본 위치 저장 (대전 충남대학교)
            geoService.saveUserLocation(
                    dbMember.getEmail(),
                    127.3445, // 경도
                    36.3659  // 위도
            );
        }
        String  memberEmail = dbMember.getEmail();

        return jwtService.createJWT(memberEmail);
    }

    // 이메일로 사용자 검색
    public Member findMemberByEmail(String email) {
        return memberRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("Member not found with email: " + email));
    }

    public void updatePushNotificationSetting(String email, boolean pushEnabled) {
        Member member = memberRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("Member not found with email: " + email));

        member.setPushEnabled(pushEnabled);
        memberRepository.save(member);
    }

    // Push 알림 활성화된 사용자 필터링
    public List<String> filterPushEnabledUsers(List<String> userEmails) {
        return userEmails.stream()
                .filter(email -> {
                    // 멤버 정보를 Redis에서 조회
                    Member member = memberRepository.findById(email)
                            .orElse(null);

                    // pushEnabled 값이 true인 경우만 포함
                    return member != null && member.isPushEnabled();
                })
                .collect(Collectors.toList());
    }

    /**
     * 이메일을 기반으로 블록체인 계정 ID 조회
     *
     * @param email 사용자 이메일
     * @return 블록체인 계정 ID
     */
    public String getAccountIdByEmail(String email) {
        String redisKey = "acc_id:" + email;
        String accountId = (String) redisTemplate.opsForValue().get(redisKey);
        System.out.println("[DEBUG] Redis에서 조회한 accountId: " + accountId);
        return accountId;
    }

}