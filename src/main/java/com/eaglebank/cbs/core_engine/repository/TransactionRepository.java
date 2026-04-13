package com.eaglebank.cbs.core_engine.repository;

import com.eaglebank.cbs.core_engine.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountNumberAndDeletedFalse(String accountNumber);

    Optional<Transaction> findByIdAndAccountNumberAndDeletedFalse(String id, String accountNumber);
}
