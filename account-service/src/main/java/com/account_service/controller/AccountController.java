package com.account_service.controller;

import com.account_service.dto.AccountDTO;
import com.account_service.dto.TransactionsDTO;
import com.account_service.entity.Account;
import com.account_service.repository.AccountRepository;
import com.account_service.requestORresponse.EventRequest;
import com.account_service.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping(value="/{accountId}/transactions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody EventRequest eventRequest,
                                                    @PathVariable String accountId){
        try{
            return accountService.processTransaction(eventRequest, accountId);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccountByAccountId(@PathVariable String accountId) {
        Account account = accountService.findByAccountId(accountId);
        if (account != null) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountId){
        Account account = getAccountByAccountId(accountId).getBody();

        if (account != null) {
            return ResponseEntity.ok(account.getBalance());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}

