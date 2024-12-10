package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequest {
    private String senderEmail; // 신고를 보낸 사용자의 이메일
    private Long reportId;    // 신고 ID
    private boolean vote;
}
