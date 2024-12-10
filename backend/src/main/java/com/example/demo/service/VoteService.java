package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> jsonRedisTemplate; // JSON 전용 템플릿 추가

    private final ReportService reportService;
    private final BlockchainService blockchainService;
    private static final String BLOCKCHAIN_BASE_URL = "http://3.227.108.238:8080"; // 블록체인 서버 주소


    public void initializeVote(String reporterEmail, Long reportId, int totalUsers) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;
        Map<String, String> initialData = new HashMap<>();
        initialData.put("totalUsers", String.valueOf(totalUsers));
        initialData.put("yesVotes", "0");
        initialData.put("noVotes", "0");

        redisTemplate.opsForHash().putAll(voteKey, initialData);
        redisTemplate.expire(voteKey, 1, TimeUnit.MINUTES); // 투표 제한 시간 설정 (1분)
    }

    public boolean registerVote(String reporterEmail, Long reportId, String voterEmail, boolean vote) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;

        // 해당 신고에 대한 투표가 만료되었는지 확인
        if (Boolean.FALSE.equals(redisTemplate.hasKey(voteKey))) {
            System.out.println("[ERROR] 투표가 만료되었습니다.");
            return false;
        }

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
        int updatedValue = (currentValue != null) ? Integer.parseInt(currentValue) : 0;
        redisTemplate.opsForHash().put(voteKey, field, String.valueOf(updatedValue + 1));
    }

    private void checkVoteOutcome(String reporterEmail, Long reportId) {
        String voteKey = "report_votes:" + reporterEmail + ":" + reportId;
        String totalUsersStr = (String) redisTemplate.opsForHash().get(voteKey, "totalUsers");
        String yesVotesStr = (String) redisTemplate.opsForHash().get(voteKey, "yesVotes");

        int totalUsers = (totalUsersStr != null) ? Integer.parseInt(totalUsersStr) : 0;
        int yesVotes = (yesVotesStr != null) ? Integer.parseInt(yesVotesStr) : 0;


        // 신뢰 임계값: 절반 이상일 경우
        if (yesVotes > totalUsers / 2) {
            markAsTrusted(reporterEmail, reportId);
        }
    }

    private void markAsTrusted(String reporterEmail, Long reportId) {
        String accountId = blockchainService.getAccountIdByEmail(reporterEmail);

        if (accountId != null) {
            System.out.println("[DEBUG] 신고 ID " + reportId + "가 신뢰할 수 있는 신고로 확인되었습니다.");

            // 거래 생성 및 토큰 지급
            String apiUrl = BLOCKCHAIN_BASE_URL+"/api/ethereum/send";

            long amount = 1L;

            // RestTemplate 생성 config 파일에 bean 없는 것 같아서 일단은 그냥 생성
            RestTemplate restTemplate = new RestTemplate();

            try {
                // 파라미터를 URL에 추가하여 호출
                // GET 요청을 보내기 위해 쿼리 파라미터 추가
                String url = apiUrl + "?toAddress=" + accountId + "&amount=" + amount;

                // GET 요청 실행
                ResponseEntity<String> response = restTemplate.postForEntity(url,null, String.class);


                // 응답 처리
                if (response.getStatusCode().is2xxSuccessful()) {
                    // 응답 성공 -> user에게 알려주기 + 보여주는 토큰 양 업데이트 + 신고 리포트 업데이트 필요
                    System.out.println("[DEBUG] API 호출 성공: " + response.getBody());

                    // Report 신뢰 업데이트
                    reportService.markReportAsTrusted(reporterEmail, reportId);

                } else {
                    System.err.println("[ERROR] API 호출 실패: " + response.getStatusCode());
                    System.err.println("[ERROR] 응답 내용: " + response.getBody());
                }
            } catch (Exception e) {
                System.err.println("[ERROR] API 호출 중 오류 발생: " + e.getMessage());
            }

        } else {
            System.out.println("[ERROR] "+reporterEmail+"의 accountId 찾을 수 없다!");
        }
    }
}
