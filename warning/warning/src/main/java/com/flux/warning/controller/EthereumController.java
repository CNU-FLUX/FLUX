package com.flux.warning.controller;

import com.flux.warning.service.EthereumService;
//import com.flux.warning.service.EthereumServiceNew;

import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/ethereum")
public class EthereumController {
    private final EthereumService ethereumService;
//    private final EthereumServiceNew ethereumServiceNew;
    public EthereumController(EthereumService ethereumService) {
        this.ethereumService = ethereumService;
//        this.ethereumServiceNew=ethereumServiceNew;
    }

    @PostMapping("/send")
    public TransactionReceipt sendTransaction(@RequestParam String toAddress, @RequestParam BigDecimal amount) {
        try {
            return ethereumService.sendTransaction(toAddress, amount);
        } catch (Exception e) {
            throw new RuntimeException("트랜잭션 전송 실패", e);
        }
    }
    // 특정 주소로 현재 시간 메시지를 전송하는 엔드포인트
//    @GetMapping("/send-time-message")
//    public String sendCurrentTimeMessage(@RequestParam String toAddress) {
//        try {
//            // EthereumService의 메서드를 호출하여 트랜잭션 전송
//            TransactionReceipt receipt = ethereumService.sendCurrentTimeMessage(toAddress);
//            return "Message sent! Transaction Hash: " + receipt.getTransactionHash();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Failed to send message: " + e.getMessage();
//        }
//    }
    @GetMapping("/get-message")
    public String getMessage(@RequestParam String transactionHash) {
        try {
            // 트랜잭션 해시를 통해 메시지 가져오기
            String message = ethereumService.getMessageFromTransactionHash(transactionHash);
            return "Message: " + message;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get message: " + e.getMessage();
        }
    }

}
