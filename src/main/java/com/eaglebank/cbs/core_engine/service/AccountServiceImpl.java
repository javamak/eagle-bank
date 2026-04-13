package com.eaglebank.cbs.core_engine.service;

import com.eaglebank.cbs.core_engine.api.AccountApiDelegate;
import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse.AccountTypeEnum;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse.CurrencyEnum;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse.SortCodeEnum;
import com.eaglebank.cbs.core_engine.model.CreateBankAccountRequest;
import com.eaglebank.cbs.core_engine.model.ListBankAccountsResponse;
import com.eaglebank.cbs.core_engine.model.UpdateBankAccountRequest;
import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountApiDelegate {

  private final BankAccountRepository bankAccountRepository;
  private final UserRepository userRepository;

  @Override
  public ResponseEntity<BankAccountResponse> createAccount(
      CreateBankAccountRequest createBankAccountRequest) {
    String loggedInUserId = getLoggedUserId();

    String accountNumber = generateAccountNumber();
    BankAccount account = new BankAccount();
    account.setAccountNumber(accountNumber);
    account.setSortCode("10-10-10");
    account.setName(createBankAccountRequest.getName());
    account.setAccountType(createBankAccountRequest.getAccountType().getValue());
    account.setBalance(0.00);
    account.setCurrency("GBP");
    account.setUserId(loggedInUserId);

    BankAccount saved = bankAccountRepository.save(account);
    return ResponseEntity.status(HttpStatus.CREATED).body(toBankAccountResponse(saved));
  }

  @Override
  public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
    String loggedInUserId = getLoggedUserId();

    BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

    if (!account.getUserId().equals(loggedInUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this account");
    }

    account.setDeleted(true);
    bankAccountRepository.save(account);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
    String loggedInUserId = getLoggedUserId();

    BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

    if (!account.getUserId().equals(loggedInUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account");
    }

    return ResponseEntity.ok(toBankAccountResponse(account));
  }

  @Override
  public ResponseEntity<ListBankAccountsResponse> listAccounts() {
    String loggedInUserId = getLoggedUserId();

    List<BankAccountResponse> accounts = bankAccountRepository
        .findByUserIdAndDeletedFalse(loggedInUserId)
        .stream()
        .map(this::toBankAccountResponse)
        .toList();

    return ResponseEntity.ok(new ListBankAccountsResponse(accounts));
  }

  @Override
  public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
      String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
    String loggedInUserId = getLoggedUserId();

    BankAccount account = bankAccountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));

    if (!account.getUserId().equals(loggedInUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this account");
    }

    if (updateBankAccountRequest.getName() != null) {
      account.setName(updateBankAccountRequest.getName());
    }
    if (updateBankAccountRequest.getAccountType() != null) {
      account.setAccountType(updateBankAccountRequest.getAccountType().getValue());
    }

    BankAccount saved = bankAccountRepository.save(account);
    return ResponseEntity.ok(toBankAccountResponse(saved));
  }

  private String getLoggedUserId() {
    return (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

  }

  private String generateAccountNumber() {
    Random random = new Random();
    int digits = 100000 + random.nextInt(900000); // 6 random digits
    String accountNumber = "01" + digits;
    // Ensure uniqueness
    while (bankAccountRepository.existsById(accountNumber)) {
      digits = 100000 + random.nextInt(900000);
      accountNumber = "01" + digits;
    }
    return accountNumber;
  }

  private BankAccountResponse toBankAccountResponse(BankAccount account) {
    return new BankAccountResponse(
        account.getAccountNumber(),
        SortCodeEnum.fromValue(account.getSortCode()),
        account.getName(),
        AccountTypeEnum.fromValue(account.getAccountType()),
        account.getBalance(),
        CurrencyEnum.fromValue(account.getCurrency()),
        toOffsetDateTime(account.getCreatedTimestamp()),
        toOffsetDateTime(account.getUpdatedTimestamp()));
  }

  private OffsetDateTime toOffsetDateTime(Instant timestamp) {
    return timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC);
  }
}
