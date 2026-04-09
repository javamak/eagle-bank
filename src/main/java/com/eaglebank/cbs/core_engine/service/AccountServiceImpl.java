package com.eaglebank.cbs.core_engine.service;

import com.eaglebank.cbs.core_engine.api.AccountApiDelegate;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse;
import com.eaglebank.cbs.core_engine.model.CreateBankAccountRequest;
import com.eaglebank.cbs.core_engine.model.ListBankAccountsResponse;
import com.eaglebank.cbs.core_engine.model.UpdateBankAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountApiDelegate {

  @Override
  public ResponseEntity<BankAccountResponse> createAccount(
      CreateBankAccountRequest createBankAccountRequest) {
    return AccountApiDelegate.super.createAccount(createBankAccountRequest);
  }

  @Override
  public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
    return AccountApiDelegate.super.deleteAccountByAccountNumber(accountNumber);
  }

  @Override
  public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
    return AccountApiDelegate.super.fetchAccountByAccountNumber(accountNumber);
  }

  @Override
  public ResponseEntity<ListBankAccountsResponse> listAccounts() {
    return AccountApiDelegate.super.listAccounts();
  }

  @Override
  public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
      String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
    return AccountApiDelegate.super.updateAccountByAccountNumber(
        accountNumber, updateBankAccountRequest);
  }
}
