package com.account_service.service;

import com.account_service.dto.TransactionsDTO;
import com.account_service.entity.Account;
import com.account_service.entity.EventStatus;
import com.account_service.entity.EventType;
import com.account_service.entity.Transactions;
import com.account_service.repository.AccountRepository;
import com.account_service.repository.TransactionsRepository;
import com.account_service.requestORresponse.EventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionsRepository transactionsRepository;

    public ResponseEntity<String>  processTransaction(EventRequest eventRequest,  String accountId) {

        if(eventRequest == null){
            throw new IllegalArgumentException("Invalid Transaction");
        }
        if(!accountId.equals(eventRequest.accountId())){
            throw new IllegalArgumentException("Invalid account details");
        }

        Account account = findByAccountId(eventRequest.accountId());
        if(account == null){
            throw new RuntimeException("Account not found.");
        }
        BigDecimal balance = account.getBalance();

        if(EventType.DEBIT.equals(eventRequest.type())){
            if(account.getBalance().compareTo(eventRequest.amount()) <= 0){
                return ResponseEntity.badRequest().body("Insufficient balance");
            }
        }

        if(EventType.CREDIT.equals(eventRequest.type())){
            balance  = balance.add(eventRequest.amount());
        }else{
            balance= balance.subtract(eventRequest.amount());
        }
        account.setBalance(balance);
        Account saveAccount = accountRepository.save(account);

        Transactions transactions = buildTransaction(eventRequest);
        transactions.setAccount(saveAccount);
        transactionsRepository.save(transactions);

        return ResponseEntity.ok("Transaction processed successfully");

    }

    private Transactions buildTransaction(EventRequest eventRequest) {
        Transactions transactions = new Transactions();
        transactions.setTransactionAmount(eventRequest.amount());
        transactions.setTransactionStatus(EventStatus.PROCESSED);
        transactions.setTransactionTime(eventRequest.eventTimestamp());
        transactions.setProcessingTime(LocalDateTime.now());
        transactions.setType(eventRequest.type());
        return transactions;
    }

    public Account findByAccountId(String accountId){
        if(accountId == null){
            throw new NullPointerException("Cannot process. accountId is null");
        }
        Optional<Account> account = accountRepository.findByAccountId(accountId);
        return account.orElse(null);
    }
}
