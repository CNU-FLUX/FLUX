package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final RedisTemplate<String, Object> redisTemplate;
    public void initializeVote(String reporterEmail, Long reportId, int totalUsers) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;
        redisTemplate.opsForHash().put(voteKey, "totalUsers", String.valueOf(totalUsers));
        redisTemplate.opsForHash().put(voteKey, "yesVotes", "0");
        redisTemplate.opsForHash().put(voteKey, "noVotes", "0");
        redisTemplate.expire(voteKey, 1, TimeUnit.MINUTES); // 투표 제한 시간 설정 (1분)
    }

    public boolean registerVote(String reporterEmail, Long reportId, String voterEmail, boolean vote) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;

        // 투표자가 이미 투표했는지 확인
        Boolean hasVoted = redisTemplate.opsForHash().hasKey(voteKey, voterEmail);
        if (Boolean.TRUE.equals(hasVoted)) {
            return false; // 이미 투표한 경우
        }

        // 투표 기록 저장
        redisTemplate.opsForHash().put(voteKey, voterEmail, vote ? "yes" : "no");

        // 투표 집계
        if (vote) {
            incrementField(voteKey, "yesVotes");
        } else {
            incrementField(voteKey, "noVotes");
        }

        // 투표 결과 체크
        checkVoteOutcome(reporterEmail, reportId);

        return true;
    }

    private void incrementField(String voteKey, String field) {
        // 현재 값을 String으로 가져와서 증가시키고 다시 저장
        String currentValue = (String) redisTemplate.opsForHash().get(voteKey, field);
        int updatedValue = Integer.parseInt(currentValue) + 1;
        redisTemplate.opsForHash().put(voteKey, field, String.valueOf(updatedValue));
    }

    private void checkVoteOutcome(String reporterEmail, Long reportId) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;
        int totalUsers = (int) redisTemplate.opsForHash().get(voteKey, "totalUsers");
        int yesVotes = (int) redisTemplate.opsForHash().get(voteKey, "yesVotes");

        // 신뢰 임계값: 절반 이상일 경우
        if (yesVotes > totalUsers / 2) {
            markAsTrusted(reporterEmail, reportId);
        }
    }

    private void markAsTrusted(String reporterEmail, Long reportId) {
        System.out.println("[DEBUG] 신고 ID " + reportId + "가 신뢰할 수 있는 신고로 확인되었습니다.");
        // TODO: 블록체인 전송 또는 후속 처리
    }
}
