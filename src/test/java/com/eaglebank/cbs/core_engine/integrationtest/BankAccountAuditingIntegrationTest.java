package com.eaglebank.cbs.core_engine.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eaglebank.cbs.core_engine.entity.BankAccount;
import java.time.Instant;

import com.eaglebank.cbs.core_engine.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:auditing-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.show-sql=false"
    })
class BankAccountAuditingIntegrationTest {

  @Autowired private BankAccountRepository bankAccountRepository;

  @Test
  void saveAndUpdate_shouldPopulateCreatedAndUpdatedTimestampsAutomatically() throws Exception {
    BankAccount account = new BankAccount();
    account.setAccountNumber("01111111");
    account.setSortCode("10-10-10");
    account.setName("Audit Test Account");
    account.setAccountType("personal");
    account.setBalance(100.0);
    account.setCurrency("GBP");
    account.setUserId("usr-1");

    BankAccount saved = bankAccountRepository.saveAndFlush(account);

    assertNotNull(saved.getCreatedTimestamp());
    assertNotNull(saved.getUpdatedTimestamp());
    assertEquals(saved.getCreatedTimestamp(), saved.getUpdatedTimestamp());

    Instant createdAt = saved.getCreatedTimestamp();
    Instant firstUpdatedAt = saved.getUpdatedTimestamp();

    Thread.sleep(5L);

    saved.setName("Audit Test Account Updated");
    BankAccount updated = bankAccountRepository.saveAndFlush(saved);

    assertEquals(createdAt, updated.getCreatedTimestamp());
    assertTrue(updated.getUpdatedTimestamp().isAfter(firstUpdatedAt));
  }
}

