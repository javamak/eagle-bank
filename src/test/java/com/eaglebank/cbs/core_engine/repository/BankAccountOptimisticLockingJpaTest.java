package com.eaglebank.cbs.core_engine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.CreateTransactionRequest;
import com.eaglebank.cbs.core_engine.service.TransactionServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
class BankAccountOptimisticLockingJpaTest {

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
  void secondConcurrentLikeUpdate_shouldSurfaceOptimisticLockConflict() {
    User user = new User();
    user.setId("usr-1");
    user.setUsername("testuser");

    BankAccount account = new BankAccount();
    account.setAccountNumber("01234567");
    account.setUserId("usr-1");
    account.setBalance(100.0);
    account.setSortCode("10-10-10");
    account.setName("Personal Account");
    account.setAccountType("personal");
    account.setCurrency("GBP");
    account.setCreatedTimestamp(Instant.now());
    account.setUpdatedTimestamp(Instant.now());

    CreateTransactionRequest request =
        new CreateTransactionRequest(
            10.0,
            CreateTransactionRequest.CurrencyEnum.GBP,
            CreateTransactionRequest.TypeEnum.DEPOSIT);

    when(userRepository.findByUsernameAndDeletedFalse("testuser")).thenReturn(Optional.of(user));
    when(bankAccountRepository.findByAccountNumberAndDeletedFalse("01234567"))
        .thenReturn(Optional.of(account));

    AtomicInteger saveCount = new AtomicInteger();
    when(bankAccountRepository.saveAndFlush(any(BankAccount.class)))
        .thenAnswer(
            invocation -> {
              if (saveCount.incrementAndGet() == 2) {
                throw new ObjectOptimisticLockingFailureException(BankAccount.class, "01234567");
              }
              return invocation.getArgument(0);
            });

    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertEquals(201, transactionService.createTransaction("01234567", request).getStatusCode().value());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> transactionService.createTransaction("01234567", request));

    assertEquals(409, ex.getStatusCode().value());
  }
}
