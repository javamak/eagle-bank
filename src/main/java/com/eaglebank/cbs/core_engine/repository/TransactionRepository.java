package com.eaglebank.cbs.core_engine.repository;

import com.eaglebank.cbs.core_engine.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountNumber(String accountNumber);

    Optional<Transaction> findByIdAndAccountNumber(String id, String accountNumber);
}
