package com.flux.warning.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
//import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EthereumService {
    private final Web3j web3j;
    private final Credentials credentials;
    private final RawTransactionManager transactionManager;
    public EthereumService() {
        // 로컬 이더리움 노드에 연결
        this.web3j = Web3j.build(new HttpService("http://localhost:7545"));

        // 개인키를 사용하여 자격 증명 생성 (from address의 private key 사용)
        this.credentials = Credentials.create("0xadd53f9a7e588d003326d1cbf9e4a43c061aadd9bc938c843a79e7b4fd2ad743");

        this.transactionManager = new RawTransactionManager(web3j, credentials);
    }
    public TransactionReceipt sendTransaction(String toAddress, BigDecimal amount) throws Exception {
        // 트랜잭션 매니저 생성
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials);

        // 이더를 전송
        TransactionReceipt transactionReceipt = Transfer.sendFunds(
                web3j, credentials, toAddress, amount, Convert.Unit.ETHER).send();

        return transactionReceipt;
    }
    public BigInteger getNonce(String fromAddress) throws Exception {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).send();
        return ethGetTransactionCount.getTransactionCount();
    }


//    public TransactionReceipt sendCurrentTimeMessage(String toAddress) throws Exception {
//        BigInteger value = BigInteger.ZERO;
//        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
//
//        // 현재 시간 가져오기 및 형식 지정
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String currentTimeMessage = "Current time: " + now.format(formatter);
//
//        // 메시지를 Hex 형식으로 변환
//        String data = Numeric.toHexString(currentTimeMessage.getBytes());
//
//        // 올바른 nonce 값 가져오기
//        BigInteger nonce = web3j.ethGetTransactionCount(
//                credentials.getAddress(), DefaultBlockParameterName.LATEST
//        ).send().getTransactionCount();
//
//        // 가스 제한을 위한 추정 트랜잭션 생성
//        BigInteger gasLimit = web3j.ethEstimateGas(
//                Transaction.createFunctionCallTransaction(credentials.getAddress(), nonce, gasPrice, null, toAddress, value, data)
//        ).send().getAmountUsed();
//
//        // 트랜잭션 생성
//        RawTransaction rawTransaction = RawTransaction.createTransaction(
//                nonce, gasPrice, gasLimit, toAddress, value, data
//        );
//
//        // 트랜잭션 서명
//        String signedTransaction;
//        try {
//            signedTransaction = transactionManager.sign(rawTransaction);
//            System.out.println("Signed transaction: " + signedTransaction);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to sign transaction", e);
//        }
//
//        // 서명된 트랜잭션 전송
//        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTransaction).send();
//
//        // 전송 오류 확인
//        if (ethSendTransaction.hasError()) {
//            throw new RuntimeException("Transaction failed: " + ethSendTransaction.getError().getMessage());
//        }
//
//        // 트랜잭션 해시 가져오기
//        String transactionHash = ethSendTransaction.getTransactionHash();
//        if (transactionHash == null) {
//            throw new RuntimeException("Failed to send transaction: No transaction hash returned");
//        }
//        System.out.println("Transaction hash: " + transactionHash);
//
//        // 타임아웃 설정 (예: 30초)
//        long startTime = System.currentTimeMillis();
//        Optional<TransactionReceipt> receiptOptional;
//
//        while (System.currentTimeMillis() - startTime < 30000) {  // 30초 동안 대기
//            receiptOptional = web3j.ethGetTransactionReceipt(transactionHash).send().getTransactionReceipt();
//            if (receiptOptional.isPresent()) {
//                return receiptOptional.get();  // 영수증이 생성되면 반환
//            }
//            Thread.sleep(1000);  // 1초 대기 후 재시도
//        }
//
//        throw new RuntimeException("Transaction receipt not generated within 30 seconds");
//    }
public String getMessageFromTransactionHash(String transactionHash) throws Exception {
    // 트랜잭션 해시로 트랜잭션 정보 가져오기
    EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();

    // 트랜잭션이 존재하는지 확인
    if (ethTransaction.getTransaction().isPresent()) {
        // 트랜잭션 정보
        Transaction transaction = ethTransaction.getTransaction().get();

        // 트랜잭션의 input 데이터 (메시지)
        String input = transaction.getInput();

        // input은 0x로 시작하는 Hex 데이터이므로, 이를 ByteArray로 변환하여 메시지를 복원
        String message = new String(Numeric.hexStringToByteArray(input));

        return message;
    } else {
        throw new Exception("Transaction not found for hash: " + transactionHash);
    }
}






}
