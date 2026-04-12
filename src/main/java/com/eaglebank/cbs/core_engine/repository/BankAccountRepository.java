package com.eaglebank.cbs.core_engine.repository;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

    List<BankAccount> findByUserId(String userId);
}

