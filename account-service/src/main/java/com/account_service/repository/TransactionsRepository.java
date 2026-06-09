package com.account_service.repository;

import com.account_service.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    
    List<Transactions> findByAccountId(Long accountId);
}

