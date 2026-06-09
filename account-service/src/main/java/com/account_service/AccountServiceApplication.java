package com.account_service;

import com.account_service.entity.Account;
import com.account_service.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.util.LinkedList;

@SpringBootApplication
public class AccountServiceApplication implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        Account account = new Account();

        account.setAccountId("ACC123456");
                account.setBalance(new BigDecimal("1000.00"));
                account.setTransactionsList(new LinkedList<>());
                accountRepository.save(account);
    }
}
