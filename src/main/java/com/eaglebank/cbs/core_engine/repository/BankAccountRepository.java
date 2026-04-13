package com.eaglebank.cbs.core_engine.repository;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

    Optional<BankAccount> findByAccountNumberAndDeletedFalse(String accountNumber);

    List<BankAccount> findByUserIdAndDeletedFalse(String userId);
}
