package com.eaglebank.cbs.core_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.entity.Transaction;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.CreateTransactionRequest;
import com.eaglebank.cbs.core_engine.model.ListTransactionsResponse;
import com.eaglebank.cbs.core_engine.model.TransactionResponse;
import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import com.eaglebank.cbs.core_engine.repository.TransactionRepository;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private BankAccountRepository bankAccountRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private TransactionServiceImpl transactionService;

  @BeforeEach
  void setUpSecurityContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createTransaction_deposit_updatesBalanceAndReturnsCreated() {
    User user = buildUser("usr-1", "testuser");
    BankAccount account = buildAccount("01234567", "usr-1", 100.0);
    CreateTransactionRequest request =
        new CreateTransactionRequest(
            25.0,
            CreateTransactionRequest.CurrencyEnum.GBP,
            CreateTransactionRequest.TypeEnum.DEPOSIT);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));
    when(bankAccountRepository.saveAndFlush(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

    var response = transactionService.createTransaction("01234567", request);

    assertEquals(201, response.getStatusCode().value());
    TransactionResponse body = response.getBody();
    assertEquals(25.0, body.getAmount());
    assertEquals(TransactionResponse.TypeEnum.DEPOSIT, body.getType());
    assertEquals(125.0, account.getBalance());
    verify(bankAccountRepository).saveAndFlush(account);
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void createTransaction_withdrawalWithInsufficientFunds_returns422() {
    User user = buildUser("usr-1", "testuser");
    BankAccount account = buildAccount("01234567", "usr-1", 50.0);
    CreateTransactionRequest request =
        new CreateTransactionRequest(
            75.0,
            CreateTransactionRequest.CurrencyEnum.GBP,
            CreateTransactionRequest.TypeEnum.WITHDRAWAL);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> transactionService.createTransaction("01234567", request));

    assertEquals(422, ex.getStatusCode().value());
    assertEquals("Insufficient funds", ex.getReason());
  }

  @Test
  void createTransaction_whenAccountNotOwnedByUser_returns403() {
    User user = buildUser("usr-1", "testuser");
    BankAccount account = buildAccount("01234567", "usr-2", 100.0);
    CreateTransactionRequest request =
        new CreateTransactionRequest(
            20.0,
            CreateTransactionRequest.CurrencyEnum.GBP,
            CreateTransactionRequest.TypeEnum.DEPOSIT);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> transactionService.createTransaction("01234567", request));

    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void createTransaction_whenOptimisticLockConflict_returns409() {
    User user = buildUser("usr-1", "testuser");
    BankAccount account = buildAccount("01234567", "usr-1", 100.0);
    CreateTransactionRequest request =
        new CreateTransactionRequest(
            10.0,
            CreateTransactionRequest.CurrencyEnum.GBP,
            CreateTransactionRequest.TypeEnum.DEPOSIT);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));
    when(bankAccountRepository.saveAndFlush(any(BankAccount.class)))
        .thenThrow(new ObjectOptimisticLockingFailureException(BankAccount.class, "01234567"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> transactionService.createTransaction("01234567", request));

    assertEquals(409, ex.getStatusCode().value());
    assertEquals("Account balance was updated by another request. Please retry.", ex.getReason());
  }

  @Test
  void listAccountTransaction_returnsOnlyMappedTransactionsForOwnedAccount() {
    User user = buildUser("usr-1", "testuser");
    BankAccount account = buildAccount("01234567", "usr-1", 100.0);

    Transaction tx = new Transaction();
    tx.setId("tan-123abc123def");
    tx.setAccountNumber("01234567");
    tx.setUserId("usr-1");
    tx.setAmount(30.0);
    tx.setCurrency("GBP");
    tx.setType("deposit");
    tx.setCreatedTimestamp(Instant.now());

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));
    when(transactionRepository.findByAccountNumber("01234567")).thenReturn(List.of(tx));

    var response = transactionService.listAccountTransaction("01234567");

    assertEquals(200, response.getStatusCode().value());
    ListTransactionsResponse body = response.getBody();
    assertEquals(1, body.getTransactions().size());
    assertEquals("tan-123abc123def", body.getTransactions().get(0).getId());
  }

  private User buildUser(String id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }

  private BankAccount buildAccount(String accountNumber, String userId, Double balance) {
    BankAccount account = new BankAccount();
    account.setAccountNumber(accountNumber);
    account.setUserId(userId);
    account.setBalance(balance);
    account.setSortCode("10-10-10");
    account.setName("Personal Account");
    account.setAccountType("personal");
    account.setCurrency("GBP");
    account.setCreatedTimestamp(Instant.now());
    account.setUpdatedTimestamp(Instant.now());
    return account;
  }
}
