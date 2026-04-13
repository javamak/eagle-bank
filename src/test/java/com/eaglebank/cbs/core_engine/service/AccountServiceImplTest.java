package com.eaglebank.cbs.core_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.model.BankAccountResponse;
import com.eaglebank.cbs.core_engine.model.CreateBankAccountRequest;
import com.eaglebank.cbs.core_engine.model.ListBankAccountsResponse;
import com.eaglebank.cbs.core_engine.model.UpdateBankAccountRequest;
import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock private BankAccountRepository bankAccountRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private AccountServiceImpl accountService;

  @BeforeEach
  void setUpSecurityContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("usr-1", null));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createAccount_success_returns201AndPersistsDefaults() {
    CreateBankAccountRequest request =
        new CreateBankAccountRequest("My Account", CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

    when(bankAccountRepository.existsById(any(String.class))).thenReturn(false);
    when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = accountService.createAccount(request);

    assertEquals(201, response.getStatusCode().value());
    BankAccountResponse body = response.getBody();
    assertEquals("My Account", body.getName());
    assertEquals("10-10-10", body.getSortCode().getValue());
    assertEquals("GBP", body.getCurrency().getValue());
    assertEquals(0.0, body.getBalance());

    ArgumentCaptor<BankAccount> accountCaptor = ArgumentCaptor.forClass(BankAccount.class);
    verify(bankAccountRepository).save(accountCaptor.capture());
    assertEquals("usr-1", accountCaptor.getValue().getUserId());
  }

  @Test
  void fetchAccountByAccountNumber_whenOwned_returns200() {
    BankAccount account = buildAccount("01234567", "usr-1", "Original");
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));

    var response = accountService.fetchAccountByAccountNumber("01234567");

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Original", response.getBody().getName());
  }

  @Test
  void fetchAccountByAccountNumber_whenNotOwned_returns403() {
    BankAccount account = buildAccount("01234567", "usr-2", "Original");
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> accountService.fetchAccountByAccountNumber("01234567"));

    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void listAccounts_returnsLoggedInUsersAccounts() {
    BankAccount first = buildAccount("01234567", "usr-1", "A1");
    BankAccount second = buildAccount("01765432", "usr-1", "A2");

    when(bankAccountRepository.findByUserId("usr-1")).thenReturn(List.of(first, second));

    var response = accountService.listAccounts();

    assertEquals(200, response.getStatusCode().value());
    ListBankAccountsResponse body = response.getBody();
    assertEquals(2, body.getAccounts().size());
    assertEquals("A1", body.getAccounts().get(0).getName());
  }

  @Test
  void updateAccountByAccountNumber_whenOwned_updatesFields() {
    BankAccount account = buildAccount("01234567", "usr-1", "Old Name");
    UpdateBankAccountRequest request = new UpdateBankAccountRequest();
    request.setName("New Name");
    request.setAccountType(UpdateBankAccountRequest.AccountTypeEnum.PERSONAL);

    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));
    when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = accountService.updateAccountByAccountNumber("01234567", request);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("New Name", response.getBody().getName());
    assertEquals("personal", account.getAccountType());
  }

  @Test
  void deleteAccountByAccountNumber_whenOwned_deletesAccount() {
    BankAccount account = buildAccount("01234567", "usr-1", "Original");
    when(bankAccountRepository.findById("01234567")).thenReturn(Optional.of(account));

    var response = accountService.deleteAccountByAccountNumber("01234567");

    assertEquals(204, response.getStatusCode().value());
    verify(bankAccountRepository).delete(account);
  }

  private BankAccount buildAccount(String accountNumber, String userId, String name) {
    BankAccount account = new BankAccount();
    account.setAccountNumber(accountNumber);
    account.setSortCode("10-10-10");
    account.setName(name);
    account.setAccountType("personal");
    account.setBalance(100.0);
    account.setCurrency("GBP");
    account.setUserId(userId);
    account.setCreatedTimestamp(Instant.now().minusSeconds(86400));
    account.setUpdatedTimestamp(Instant.now().minusSeconds(3600));
    return account;
  }
}
