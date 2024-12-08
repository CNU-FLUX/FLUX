package com.example.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BlockchainService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLOCKCHAIN_BASE_URL = "http://44.192.38.26:8080"; // 블록체인 서버 주소
    private static final String ACCOUNT_ID_PREFIX = "acc_id:"; // Redis 키 Prefix


    public BlockchainService(RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = new RestTemplate();
        this.redisTemplate = redisTemplate;
    }

    /**
     * Redis에서 accountId 조회
     *
     * @param email 사용자 이메일
     * @return accountId (블록체인 주소)
     */
    public String getAccountIdByEmail(String email) {
        String accountIdKey = ACCOUNT_ID_PREFIX + email;

        // Redis에서 hash 타입의 "accountId" 필드 조회
        Object accountId = redisTemplate.opsForHash().get(accountIdKey, "accountId");

        if (accountId == null || !(accountId instanceof String)) {
            throw new RuntimeException("Account ID not found or invalid for email: " + email);
        }
        System.out.println("[DEBUG]"+email+"의 블록체인 account Id : " + accountId);

        return (String) accountId;
    }

    /**
     * 블록체인에 메시지 전송 및 트랜잭션 해시 반환
     *
     * @param email    블록체인 전송 대상 주소
     * @param reportMessage 신고 메시지
     * @return 트랜잭션 해시
     */
    public String sendMessageToBlockchain(String email, String reportMessage) {
        String toAddress = getAccountIdByEmail(email); // Redis에서 accountId 조회
        String url = BLOCKCHAIN_BASE_URL + "/api/ethereum/send-message";

        Map<String, String> params = Map.of(
                "toAddress", toAddress,
                "reportMessage", reportMessage
        );

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url + "?toAddress={toAddress}&reportMessage={reportMessage}",
                Map.class,
                params
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().get("transactionHash").toString();
        } else {
            throw new RuntimeException("Failed to send message to blockchain");
        }
    }

    /**
     * 블록체인에서 메시지 조회
     *
     * @param transactionHash 트랜잭션 해시
     * @return 메시지 내용
     */
    public String getMessageFromBlockchain(String transactionHash) {
        String url = BLOCKCHAIN_BASE_URL + "/api/ethereum/get-message";

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url + "?transactionHash={transactionHash}",
                Map.class,
                Map.of("transactionHash", transactionHash)
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().get("message").toString();
        } else {
            throw new RuntimeException("Failed to retrieve message from blockchain");
        }
    }

    // 블록체인에서 토큰 잔액 조회
    public String getTokenBalance(String address) {
        String url = BLOCKCHAIN_BASE_URL + "/api/ethereum/get-token";

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url + "?address={address}",
                Map.class,
                Map.of("address", address)
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().get("balance").toString();
        } else {
            throw new RuntimeException("Failed to retrieve token balance for address: " + address);
        }
    }
}
