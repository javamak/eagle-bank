package com.eaglebank.cbs.core_engine.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eaglebank.cbs.core_engine.entity.Address;
import com.eaglebank.cbs.core_engine.entity.BankAccount;
import com.eaglebank.cbs.core_engine.entity.Transaction;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import com.eaglebank.cbs.core_engine.repository.TransactionRepository;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:soft-delete-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.show-sql=false"
    })
class SoftDeleteIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private BankAccountRepository bankAccountRepository;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void deleteUser_shouldSoftDeleteAndFilterFromFindById() {
    User user = new User();
    user.setId("usr-softdel-1");
    user.setName("User Soft Delete");
    user.setUsername("softdel-user");
    user.setPassword("encoded-password");
    user.setAddress(new Address("line1", null, null, "town", "county", "postcode"));
    user.setPhoneNumber("+447700900111");
    user.setEmail("softdel-user@example.com");
    userRepository.saveAndFlush(user);

    userRepository.delete(user);
    userRepository.flush();

    assertTrue(userRepository.findById("usr-softdel-1").isEmpty());
    Boolean deleted =
        jdbcTemplate.queryForObject("SELECT deleted FROM users WHERE id = ?", Boolean.class, "usr-softdel-1");
    assertEquals(Boolean.TRUE, deleted);
  }

  @Test
  void deleteBankAccount_shouldSoftDeleteAndFilterFromFindById() {
    BankAccount account = new BankAccount();
    account.setAccountNumber("01999999");
    account.setSortCode("10-10-10");
    account.setName("Soft Delete Account");
    account.setAccountType("personal");
    account.setBalance(100.0);
    account.setCurrency("GBP");
    account.setUserId("usr-softdel-1");
    bankAccountRepository.saveAndFlush(account);

    bankAccountRepository.delete(account);
    bankAccountRepository.flush();

    assertTrue(bankAccountRepository.findById("01999999").isEmpty());
    Boolean deleted =
        jdbcTemplate.queryForObject(
            "SELECT deleted FROM bank_accounts WHERE account_number = ?", Boolean.class, "01999999");
    assertEquals(Boolean.TRUE, deleted);
  }

  @Test
  void deleteTransaction_shouldSoftDeleteAndFilterFromFindById() {
    Transaction transaction = new Transaction();
    transaction.setId("tan-softdel-1");
    transaction.setAccountNumber("01999999");
    transaction.setUserId("usr-softdel-1");
    transaction.setAmount(12.50);
    transaction.setCurrency("GBP");
    transaction.setType("deposit");
    transaction.setReference("soft-delete");
    transactionRepository.saveAndFlush(transaction);

    transactionRepository.delete(transaction);
    transactionRepository.flush();

    assertTrue(transactionRepository.findById("tan-softdel-1").isEmpty());
    Boolean deleted =
        jdbcTemplate.queryForObject(
            "SELECT deleted FROM transactions WHERE id = ?", Boolean.class, "tan-softdel-1");
    assertEquals(Boolean.TRUE, deleted);
  }
}
