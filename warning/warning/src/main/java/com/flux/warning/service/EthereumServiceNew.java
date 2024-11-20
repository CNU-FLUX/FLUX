//package com.flux.warning.service;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.methods.response.EthTransaction;
//import org.web3j.protocol.core.methods.response.Transaction;
//import org.web3j.utils.Numeric;
//public class EthereumServiceNew {
//    private Web3j web3j;
//
//    public EthereumServiceNew(Web3j web3j) {
//        this.web3j = web3j;
//    }
//
//    // 트랜잭션 해시로 메시지 조회
//    public String getMessageFromTransactionHash(String transactionHash) throws Exception {
//        // 트랜잭션 조회
//        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
//
//        // 트랜잭션이 존재하는지 확인
//        if (ethTransaction.getTransaction().isPresent()) {
//            // 트랜잭션 객체 가져오기
//            Transaction transaction = ethTransaction.getTransaction().get();
//
//            // 트랜잭션의 input 데이터를 가져오기
//            String inputData = transaction.getInput();
//
//            // Hex 데이터를 ASCII 문자열로 변환
//            String message = Numeric.toHexString(new byte[0]).equals(inputData) ? "No data" : new String(Numeric.hexStringToByteArray(inputData));
//
//            return message;
//        } else {
//            throw new Exception("Transaction not found");
//        }
//    }
//}
